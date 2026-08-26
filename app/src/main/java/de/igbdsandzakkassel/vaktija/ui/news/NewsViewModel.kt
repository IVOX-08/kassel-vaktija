package de.igbdsandzakkassel.vaktija.ui.news

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.igbdsandzakkassel.vaktija.core.locale.LocaleController
import de.igbdsandzakkassel.vaktija.data.media.NewsImageCompressor
import de.igbdsandzakkassel.vaktija.data.model.NewsItem
import de.igbdsandzakkassel.vaktija.data.community.CommunityRepository
import de.igbdsandzakkassel.vaktija.data.model.AdminRole
import kotlinx.coroutines.flow.map
import de.igbdsandzakkassel.vaktija.data.repository.AdminController
import de.igbdsandzakkassel.vaktija.data.repository.NewsRepository
import de.igbdsandzakkassel.vaktija.data.translate.GeminiTranslator
import de.igbdsandzakkassel.vaktija.data.translate.NewsTranslator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val newsRepository: NewsRepository,
    private val geminiTranslator: GeminiTranslator,
    private val translator: NewsTranslator,
    private val imageCompressor: NewsImageCompressor,
    private val adminController: AdminController,
    private val communityRepository: CommunityRepository,
) : ViewModel() {

    // Session cache of already-fetched flyer bytes, so scrolling a card back into view doesn't
    // refetch. Only successful loads are cached (a transient failure stays retryable).
    private val imageCache = mutableMapOf<String, ByteArray>()
    private val imageMutex = Mutex()

    /** True only for the head admin — the one account allowed to reach every community. */
    val canBroadcast: StateFlow<Boolean> = adminController.observeRole()
        .map { it.canBroadcast }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /**
     * The head admin can ONLY broadcast: he has no business posting in a community's name. So for
     * him the switch is not a choice, and the dialog says where the announcement is going instead
     * of offering an option that does not exist.
     */
    val broadcastOnly: StateFlow<Boolean> = combine(
        adminController.observeRole(),
        communityRepository.observeSelection(),
    ) { role, selection -> role.canBroadcast && !role.canPostNews(selection?.community?.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Whether this account may remove [item] — its own community's, or its own broadcast. */
    fun canDelete(item: NewsItem, role: AdminRole, communityId: String?): Boolean =
        if (item.isBroadcast) role.canBroadcast else role.canPostNews(communityId)

    /** Role + selected community, for the per-item delete check. */
    val deleteContext: StateFlow<Pair<AdminRole, String?>> = combine(
        adminController.observeRole(),
        communityRepository.observeSelection(),
    ) { role, selection -> role to selection?.community?.id }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdminRole.None to null)

    /** Newest-first announcements; null while the first snapshot is still loading. */
    val news: StateFlow<List<NewsItem>?> = newsRepository.observeNews()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * Whether the signed-in account may administer the community currently being viewed.
     *
     * Both halves matter: a community admin looking at another community must see no controls, and
     * the head admin must see them everywhere. The Firestore rules enforce the same condition, so
     * this only decides what is shown — it is not the thing standing between an account and the
     * data.
     */
    val isAdmin: StateFlow<Boolean> = combine(
        adminController.observeRole(),
        communityRepository.observeSelection(),
    ) { role, selection ->
        // Either you run this community, or you are the head admin with a federation-wide notice
        // to send. Both need the composer; they write to different places.
        role.canPostNews(selection?.community?.id) || role.canBroadcast
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /**
     * Outcome of a post: whether it was saved, any languages that couldn't be translated, and
     * whether an attached image had to be dropped (couldn't be read/compressed).
     */
    data class PostOutcome(
        val ok: Boolean,
        val failedLangs: List<String>,
        val imageDropped: Boolean = false,
    )

    /**
     * Translates the announcement into every app language, optionally compresses an attached flyer,
     * then publishes it. The admin writes once (in any language); auto-detection picks the source,
     * falling back to the current UI language. Reports any languages that failed to translate, and
     * whether the picked image couldn't be attached, so the admin can be warned (e.g. posted offline).
     */
    fun postNews(
        title: String,
        body: String,
        imageUri: Uri?,
        broadcast: Boolean,
        onResult: (PostOutcome) -> Unit,
    ) {
        val fallbackLang = LocaleController.current().tag
        viewModelScope.launch {
            val result = runCatching {
                val t = title.trim()
                val b = body.trim()
                // Compress the flyer (if any) up front; a null result means it couldn't be read.
                val imageJpeg = imageUri?.let { imageCompressor.compress(it) }
                val imageDropped = imageUri != null && imageJpeg == null
                // Prefer the high-quality Gemini translation; fall back to on-device ML Kit if it's
                // not configured or fails (e.g. offline) so a post is never blocked.
                val translated = geminiTranslator.translateToAll(t, b, fallbackLang)
                    ?: translator.translateToAll(t, b, fallbackLang)
                newsRepository.postNews(
                    titleByLang = translated.titleByLang,
                    bodyByLang = translated.bodyByLang,
                    sourceLang = translated.sourceLang,
                    imageJpeg = imageJpeg,
                    broadcast = broadcast,
                )
                translated.failedLangs to imageDropped
            }
            val (failedLangs, imageDropped) = result.getOrDefault(emptyList<String>() to false)
            onResult(PostOutcome(result.isSuccess, failedLangs, imageDropped))
        }
    }

    /** Lazily fetches the flyer bytes (cache-first), caching successes for the session. */
    suspend fun loadImage(item: NewsItem): ByteArray? {
        imageMutex.withLock { imageCache[item.id] }?.let { return it }
        val bytes = newsRepository.getNewsImage(item)
        if (bytes != null) imageMutex.withLock { imageCache[item.id] = bytes }
        return bytes
    }

    fun deleteNews(item: NewsItem) {
        viewModelScope.launch { runCatching { newsRepository.deleteNews(item) } }
    }
}

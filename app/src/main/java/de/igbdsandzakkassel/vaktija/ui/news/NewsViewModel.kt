package de.igbdsandzakkassel.vaktija.ui.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.igbdsandzakkassel.vaktija.core.locale.LocaleController
import de.igbdsandzakkassel.vaktija.data.model.NewsItem
import de.igbdsandzakkassel.vaktija.data.repository.AdminController
import de.igbdsandzakkassel.vaktija.data.repository.NewsRepository
import de.igbdsandzakkassel.vaktija.data.translate.GeminiTranslator
import de.igbdsandzakkassel.vaktija.data.translate.NewsTranslator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val newsRepository: NewsRepository,
    private val geminiTranslator: GeminiTranslator,
    private val translator: NewsTranslator,
    adminController: AdminController,
) : ViewModel() {

    /** Newest-first announcements; null while the first snapshot is still loading. */
    val news: StateFlow<List<NewsItem>?> = newsRepository.observeNews()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** True while signed in as the admin → reveals the post/delete controls. */
    val isAdmin: StateFlow<Boolean> = adminController.observeIsAdmin()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Outcome of a post: whether it was saved, and any languages that couldn't be translated. */
    data class PostOutcome(val ok: Boolean, val failedLangs: List<String>)

    /**
     * Translates the announcement into every app language, then publishes it. The admin writes once
     * (in any language); auto-detection picks the source, falling back to the current UI language.
     * Reports any languages that failed to translate so the admin can be warned (e.g. posted offline).
     */
    fun postNews(title: String, body: String, onResult: (PostOutcome) -> Unit) {
        val fallbackLang = LocaleController.current().tag
        viewModelScope.launch {
            val result = runCatching {
                val t = title.trim()
                val b = body.trim()
                // Prefer the high-quality Gemini translation; fall back to on-device ML Kit if it's
                // not configured or fails (e.g. offline) so a post is never blocked.
                val translated = geminiTranslator.translateToAll(t, b, fallbackLang)
                    ?: translator.translateToAll(t, b, fallbackLang)
                newsRepository.postNews(
                    titleByLang = translated.titleByLang,
                    bodyByLang = translated.bodyByLang,
                    sourceLang = translated.sourceLang,
                )
                translated.failedLangs
            }
            onResult(PostOutcome(result.isSuccess, result.getOrDefault(emptyList())))
        }
    }

    fun deleteNews(id: String) {
        viewModelScope.launch { runCatching { newsRepository.deleteNews(id) } }
    }
}

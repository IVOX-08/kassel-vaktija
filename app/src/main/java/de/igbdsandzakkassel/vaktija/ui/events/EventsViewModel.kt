package de.igbdsandzakkassel.vaktija.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.igbdsandzakkassel.vaktija.core.locale.LocaleController
import de.igbdsandzakkassel.vaktija.data.model.EventItem
import de.igbdsandzakkassel.vaktija.data.repository.AdminController
import de.igbdsandzakkassel.vaktija.data.repository.EventsRepository
import de.igbdsandzakkassel.vaktija.data.translate.GeminiTranslator
import de.igbdsandzakkassel.vaktija.data.translate.NewsTranslator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val eventsRepository: EventsRepository,
    private val geminiTranslator: GeminiTranslator,
    private val translator: NewsTranslator,
    adminController: AdminController,
) : ViewModel() {

    /** Soonest-first events; null while the first snapshot is loading. */
    val events: StateFlow<List<EventItem>?> = eventsRepository.observeEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val isAdmin: StateFlow<Boolean> = adminController.observeIsAdmin()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    data class PostOutcome(val ok: Boolean, val failedLangs: List<String>)

    /** Translates the event into every app language, then publishes it with its date/time. */
    fun postEvent(title: String, body: String, eventAt: Long, onResult: (PostOutcome) -> Unit) {
        val fallbackLang = LocaleController.current().tag
        viewModelScope.launch {
            val result = runCatching {
                val t = title.trim()
                val b = body.trim()
                val translated = geminiTranslator.translateToAll(t, b, fallbackLang)
                    ?: translator.translateToAll(t, b, fallbackLang)
                eventsRepository.postEvent(
                    titleByLang = translated.titleByLang,
                    bodyByLang = translated.bodyByLang,
                    sourceLang = translated.sourceLang,
                    eventAt = eventAt,
                )
                translated.failedLangs
            }
            onResult(PostOutcome(result.isSuccess, result.getOrDefault(emptyList())))
        }
    }

    fun deleteEvent(id: String) {
        viewModelScope.launch { runCatching { eventsRepository.deleteEvent(id) } }
    }
}

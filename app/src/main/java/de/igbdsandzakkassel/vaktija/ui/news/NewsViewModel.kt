package de.igbdsandzakkassel.vaktija.ui.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.igbdsandzakkassel.vaktija.data.model.NewsItem
import de.igbdsandzakkassel.vaktija.data.repository.AdminController
import de.igbdsandzakkassel.vaktija.data.repository.NewsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val newsRepository: NewsRepository,
    adminController: AdminController,
) : ViewModel() {

    /** Newest-first announcements; null while the first snapshot is still loading. */
    val news: StateFlow<List<NewsItem>?> = newsRepository.observeNews()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** True while signed in as the admin → reveals the post/delete controls. */
    val isAdmin: StateFlow<Boolean> = adminController.observeIsAdmin()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun postNews(title: String, body: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = runCatching { newsRepository.postNews(title.trim(), body.trim()) }.isSuccess
            onResult(ok)
        }
    }

    fun deleteNews(id: String) {
        viewModelScope.launch { runCatching { newsRepository.deleteNews(id) } }
    }
}

package app.royalreader.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.royalreader.domain.model.Novel
import app.royalreader.domain.repository.NovelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(val books: List<Novel> = emptyList())

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val novelRepository: NovelRepository
) : ViewModel() {
    val uiState: StateFlow<LibraryUiState> = novelRepository.observeLibrary()
        .map { LibraryUiState(books = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LibraryUiState()
        )

    fun removeFromLibrary(novelId: Long) {
        viewModelScope.launch {
            novelRepository.removeFromLibrary(novelId)
        }
    }
}
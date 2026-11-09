package app.royalreader.ui.bookdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.royalreader.domain.model.Chapter
import app.royalreader.domain.model.Novel
import app.royalreader.domain.repository.NovelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BookDetailUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val inLibrary: Boolean = false,
)

@HiltViewModel
class BookDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val novelRepository: NovelRepository,
) : ViewModel() {

    val novelID: Long = savedStateHandle.get<Long>("novelID") ?: 0L

    private val _uiState = MutableStateFlow(BookDetailUiState())
    val uiState: StateFlow<BookDetailUiState> = _uiState

    init {
        refresh(showLoading = true)
    }

    fun refresh(showLoading: Boolean = false) {
        viewModelScope.launch {
            if (showLoading) _uiState.value = _uiState.value.copy(loading = true, error = null)
            runCatching { novelRepository.getFiction(novelID) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(loading = false, error = null)
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = throwable.message ?: "Could not load novel.",
                    )
                }
        }
    }

    fun toggleLibrary() {
        val novel = novelState.value ?: return
        viewModelScope.launch {
            if (novel.inLibrary) {
                novelRepository.removeFromLibrary(novel.id)
            } else {
                novelRepository.addToLibrary(novel.copy(inLibrary = true))
            }
        }
    }

    /**
     * Fresh novel + chapter rows straight from the database.
     **/
    val novelState: StateFlow<Novel?> = novelRepository.observe(novelID)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val chapterState: StateFlow<List<Chapter>> = novelRepository.observeChapters(novelID)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

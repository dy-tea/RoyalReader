package app.royalreader.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.royalreader.domain.model.LibraryEntry
import app.royalreader.domain.repository.NovelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(val books: List<LibraryEntry> = emptyList())

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

    /**
     * Open a book directly in the reader. Resolves the resume target from the
     * local cache; refreshes the fiction once when the cache is still empty.
     * Falls back to [onFallback] (the book detail screen) when no chapter can
     * be found at all.
     **/
    fun continueReading(
        entry: LibraryEntry,
        onReady: (novelID: Long, chapterID: Long) -> Unit,
        onFallback: () -> Unit,
    ) {
        viewModelScope.launch {
            var chapter = runCatching { novelRepository.resumeChapter(entry.novel.id) }.getOrNull()
            if (chapter == null) {
                runCatching { novelRepository.getFiction(entry.novel.id) }
                chapter = runCatching { novelRepository.resumeChapter(entry.novel.id) }.getOrNull()
            }
            if (chapter != null) onReady(entry.novel.id, chapter.id) else onFallback()
        }
    }
}

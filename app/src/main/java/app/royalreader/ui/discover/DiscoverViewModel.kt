package app.royalreader.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.royalreader.domain.model.Novel
import app.royalreader.domain.repository.NovelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiscoverUiState(
    val query: String = "",
    val section: Section = Section.POPULAR,
    val books: List<Novel> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
) {
    enum class Section { POPULAR, LATEST, SEARCH }
}

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val novelRepository: NovelRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private var searchJob: kotlinx.coroutines.Job? = null

    init {
        load()
    }

    fun setSection(section: DiscoverUiState.Section) {
        _uiState.update { it.copy(section = section) }
        if (section == DiscoverUiState.Section.SEARCH) {
            search(_uiState.value.query)
        } else {
            load()
        }
    }

    fun setQuery(query: String) {
        _uiState.update { it.copy(query = query, section = DiscoverUiState.Section.SEARCH) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(400)
            search(query)
        }
    }

    fun toggleLibrary(novel: Novel) {
        viewModelScope.launch {
            if (novel.inLibrary) {
                novelRepository.removeFromLibrary(novel.id)
            } else {
                novelRepository.addToLibrary(novel.copy(inLibrary = true))
            }
            _uiState.update { state ->
                state.copy(
                    books = state.books.map { book ->
                        if (book.id == novel.id) book.copy(inLibrary = !novel.inLibrary) else book
                    }
                )
            }
        }
    }

    fun refresh() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching {
                if (_uiState.value.section == DiscoverUiState.Section.LATEST) {
                    novelRepository.latest()
                } else {
                    novelRepository.popular()
                }
            }.onSuccess { books ->
                _uiState.update {
                    it.copy(books = books, loading = false)
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(loading = false, error = throwable.message ?: "Something went wrong.")
                }
            }
        }
    }

    private fun search(query: String) = viewModelScope.launch {
        if (query.isBlank()) {
            setSection(DiscoverUiState.Section.POPULAR)
            return@launch
        }
        _uiState.update { it.copy(loading = true, error = null) }
        runCatching { novelRepository.search(query.trim()) }
            .onSuccess { books ->
                _uiState.update { it.copy(books = books, loading = false) }
            }
            .onFailure { throwable ->
                _uiState.update {
                    it.copy(loading = false, error = throwable.message ?: "Something went wrong.")
                }
            }
    }
}

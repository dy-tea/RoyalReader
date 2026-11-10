package app.royalreader.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.royalreader.domain.model.Chapter
import app.royalreader.domain.model.ReaderSettings
import app.royalreader.domain.model.ReadingProgress
import app.royalreader.domain.repository.NovelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

data class ReaderUiState(
    val novelID: Long = 0L,
    val loading: Boolean = true,
    val error: String? = null,
    val chapter: Chapter? = null,
    val previousChapter: Chapter? = null,
    val nextChapter: Chapter? = null,
    val paragraphs: List<String> = emptyList(),
    val settings: ReaderSettings = ReaderSettings(novelID = 0L),
    val restoredAnchor: Int = 0,
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val novelRepository: NovelRepository,
) : ViewModel() {

    private val novelID: Long = savedStateHandle.get<Long>("novelID") ?: 0L
    private val chapterID: Long = savedStateHandle.get<Long>("chapterID") ?: 0L

    private val _uiState = MutableStateFlow(ReaderUiState(novelID = novelID))
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    /**
     * Wall-clock time spent reading this chapter; flushed in chunks.
     **/
    private var readingTimeMs: Long = 0L

    init {
        load()
    }

    private fun load() = viewModelScope.launch {
        val settings = runCatching { novelRepository.observeReaderSettings(novelID).first() }
        val progress = runCatching { novelRepository.observeProgress(novelID).first() }
        if (settings.isSuccess) {
            _uiState.update { it.copy(settings = settings.getOrThrow()) }
        }
        val anchor = progress.getOrNull()
            ?.takeIf { it.chapterID == chapterID }?.paragraphAnchor ?: 0

        _uiState.update {
            it.copy(loading = true, error = null, restoredAnchor = anchor)
        }
        runCatching { novelRepository.getChapterContent(novelID, chapterID) }
            .onSuccess { paragraphs ->
                if (paragraphs == null) {
                    _uiState.update {
                        it.copy(loading = false, error = "Chapter is unavailable.")
                    }
                } else {
                    val chapters = novelRepository.observeChapters(novelID).first()
                    val chapter = chapters.firstOrNull { it.id == chapterID }
                    _uiState.update { state ->
                        state.copy(
                            loading = false,
                            chapter = chapter,
                            paragraphs = paragraphs,
                            previousChapter = chapters.firstOrNull {
                                it.index == (chapter?.index ?: 0) - 1
                            },
                            nextChapter = chapters.firstOrNull {
                                it.index == (chapter?.index ?: 0) + 1
                            },
                        )
                    }
                }
            }
            .onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        loading = false,
                        error = throwable.message ?: "Could not load chapter.",
                    )
                }
            }
    }

    fun retry() = load()

    /**
     * Persist a paragraph anchor when the user pauses or leaves the reader.
     * The chapter is marked read once reached.
     **/
    fun saveProgress(anchor: Int, atEnd: Boolean) {
        if (_uiState.value.paragraphs.isEmpty()) return
        viewModelScope.launch {
            novelRepository.saveProgress(progressOf(anchor))
            if (atEnd) novelRepository.markChapterRead(novelID, chapterID)
        }
    }

    private fun progressOf(anchor: Int) = ReadingProgress(
        novelID = novelID,
        chapterID = chapterID,
        paragraphAnchor = anchor,
    )

    fun changePalette(paletteID: String) {
        applySettings { it.copy(paletteID = paletteID) }
    }

    fun changeFontSize(deltaPx: Int) {
        applySettings { it.copy(fontSizePx = (it.fontSizePx + deltaPx).coerceIn(12, 36)) }
    }

    fun changeLineHeight(delta: Float) {
        applySettings { it.copy(lineHeight = (it.lineHeight + delta).coerceIn(1.0f, 2.4f)) }
    }

    private fun applySettings(transform: (ReaderSettings) -> ReaderSettings) {
        val updated = transform(_uiState.value.settings)
        _uiState.update { it.copy(settings = updated) }
        viewModelScope.launch { novelRepository.saveReaderSettings(updated) }
    }

    /**
     * Called when the reader becomes active: accrues time and flushes it to the
     * stats tables in one-minute chunks.
     **/
    fun trackReadingTime() = viewModelScope.launch {
        while (isActive) {
            delay(TICK_MS.milliseconds)
            readingTimeMs += TICK_MS
            if (readingTimeMs >= FLUSH_MS) {
                novelRepository.recordReadingTime(novelID, readingTimeMs)
                readingTimeMs = 0L
            }
        }
    }

    fun stopTrackingReadingTime() {
        val backlog = readingTimeMs
        readingTimeMs = 0L
        if (backlog > 0) {
            viewModelScope.launch { novelRepository.recordReadingTime(novelID, backlog) }
        }
    }

    companion object {
        private const val TICK_MS = 1_000L
        private const val FLUSH_MS = 60_000L
    }
}

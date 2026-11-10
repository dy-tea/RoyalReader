package app.royalreader.domain.repository

import app.royalreader.domain.model.Chapter
import app.royalreader.domain.model.DailyReadingTime
import app.royalreader.domain.model.FictionDetails
import app.royalreader.domain.model.LibraryEntry
import app.royalreader.domain.model.Novel
import app.royalreader.domain.model.ReaderSettings
import app.royalreader.domain.model.ReadingProgress
import kotlinx.coroutines.flow.Flow

interface NovelRepository {
    /**
     * Search royalroad for fictions matching [query].
     **/
    suspend fun search(query: String): List<Novel>

    /**
     * Get list of popular fictions from royalroad.
     **/
    suspend fun popular(): List<Novel>

    /**
     * Get list of recently updated fictions from royalroad.
     **/
    suspend fun latest(): List<Novel>

    /**
     * Get table of contents for fiction given [novelID], refreshing the local cache.
     **/
    suspend fun getFiction(novelID: Long): FictionDetails

    /**
     * Novels added to library by user, newest first, each with the chapter
     * the user would resume from, if any.
     */
    fun observeLibrary(): Flow<List<LibraryEntry>>

    /**
     * The chapter the reader should open when the user continues a book:
     * the saved progress chapter, else the first unread one, else chapter one.
     * Null when the table of contents is not cached yet.
     */
    suspend fun resumeChapter(novelID: Long): Chapter?

    /**
     * Observe single novel from database by [novelID], null if not found.
     */
    fun observe(novelID: Long): Flow<Novel?>

    /**
     * Add novel to library.
     */
    suspend fun addToLibrary(novel: Novel)

    /**
     * Remove from library and delete all downloaded chapters, by [novelID].
     */
    suspend fun removeFromLibrary(novelID: Long)

    /**
     * Observe table of contents for fiction given [novelID], ascending by index.
     */
    fun observeChapters(novelID: Long): Flow<List<Chapter>>

    /**
     * Paragraphs of a chapter: from local storage, else fetched and cached.
     */
    suspend fun getChapterContent(novelID: Long, chapterID: Long): List<String>?

    /**
     * Mark a chapter as read.
     */
    suspend fun markChapterRead(novelID: Long, chapterID: Long)

    /**
     * Observe reading progress for a given [novelID], null if never opened.
     */
    fun observeProgress(novelID: Long): Flow<ReadingProgress?>

    /**
     * Persist reading progress.
     */
    suspend fun saveProgress(progress: ReadingProgress)

    /**
     * Observe per-novel reader settings; falls back to defaults.
     */
    fun observeReaderSettings(novelID: Long): Flow<ReaderSettings>

    /**
     * Persist per-novel reader settings.
     */
    suspend fun saveReaderSettings(settings: ReaderSettings)

    /**
     * Record wall-clock time spent reading [novelID], for the stats calendar.
     */
    suspend fun recordReadingTime(novelID: Long, durationMs: Long)

    /**
     * Total reading time per day from [fromEpochDay] onwards.
     */
    fun observeDailyReadingTime(fromEpochDay: Long): Flow<List<DailyReadingTime>>
}

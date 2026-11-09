package app.royalreader.data.repository

import app.royalreader.data.local.db.ChapterDao
import app.royalreader.data.local.db.NovelDao
import app.royalreader.data.local.db.ProgressDao
import app.royalreader.data.local.db.ReaderSettingsDao
import app.royalreader.data.local.db.StatsDao
import app.royalreader.data.local.db.ReadingSessionEntity
import app.royalreader.data.local.db.ReaderSettingsEntity
import app.royalreader.data.local.db.toDomain
import app.royalreader.data.local.db.toEntity
import app.royalreader.data.local.files.ChapterStore
import app.royalreader.data.remote.RoyalRoadClient
import app.royalreader.domain.model.Chapter
import app.royalreader.domain.model.DailyReadingTime
import app.royalreader.domain.model.FictionDetails
import app.royalreader.domain.model.Novel
import app.royalreader.domain.model.ReaderSettings
import app.royalreader.domain.model.ReadingProgress
import app.royalreader.domain.repository.NovelRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Offline-first repository: browsing hits the network and populates the cache;
 * the library, progress and settings live entirely locally.
 **/
class NovelRepositoryImpl constructor(
    private val client: RoyalRoadClient,
    private val novelDao: NovelDao,
    private val chapterDao: ChapterDao,
    private val progressDao: ProgressDao,
    private val readerSettingsDao: ReaderSettingsDao,
    private val statsDao: StatsDao,
    private val chapterStore: ChapterStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : NovelRepository {

    /**
     * Merge cached library membership into freshly fetched novels.
     **/
    private suspend fun annotateInLibrary(novels: List<Novel>): List<Novel> =
        novels.map { novel -> novelDao.get(novel.id)?.inLibrary?.let { novel.copy(inLibrary = it) } ?: novel }

    override suspend fun search(query: String): List<Novel> = annotateInLibrary(
        withContext(ioDispatcher) { client.search(query) }
    )

    override suspend fun popular(): List<Novel> = annotateInLibrary(
        withContext(ioDispatcher) { client.popular() }
    )

    override suspend fun latest(): List<Novel> = annotateInLibrary(
        withContext(ioDispatcher) { client.latest() }
    )

    /**
     * Fetch the fiction page and refresh cached rows, preserving download/read flags.
     **/
    override suspend fun getFiction(novelID: Long): FictionDetails {
        val details = withContext(ioDispatcher) { client.getFiction(novelID) }
        val cached = novelDao.get(novelID)
        novelDao.upsert(
            listOf(details.novel.toEntity(inLibrary = cached?.inLibrary ?: false, addedToLibraryAtMs = cached?.addedToLibraryAtMs ?: 0L))
        )
        val existing = chapterDao.getForNovel(novelID).associateBy { it.id }
        chapterDao.upsert(
            details.chapters.map { chapter ->
                val previous = existing[chapter.id]
                Chapter(
                    id = chapter.id,
                    novelID = novelID,
                    title = chapter.title,
                    index = chapter.index,
                    url = chapter.url,
                    publishedAtMs = chapter.publishedAtMs,
                    downloaded = previous?.downloaded ?: false,
                    read = previous?.read ?: false,
                ).toEntity()
            }
        )
        return details
    }

    override fun observeLibrary(): Flow<List<Novel>> =
        novelDao.observeLibrary().map { list -> list.map { it.toDomain() } }

    override fun observe(novelID: Long): Flow<Novel?> =
        novelDao.observe(novelID).map { it?.toDomain() }

    override suspend fun addToLibrary(novel: Novel) {
        if (novelDao.get(novel.id) == null) {
            novelDao.upsert(listOf(novel.toEntity(inLibrary = false)))
        }
        novelDao.markInLibrary(novel.id, System.currentTimeMillis())
    }

    /**
     * Removing a novel deletes its cache entry, chapter rows,
     * downloaded files (et al) so no storage is left behind.
     **/
    override suspend fun removeFromLibrary(novelID: Long) {
        novelDao.delete(novelID)
        chapterDao.deleteForNovel(novelID)
        progressDao.deleteForNovel(novelID)
        withContext(ioDispatcher) { chapterStore.deleteNovel(novelID) }
    }

    override fun observeChapters(novelID: Long): Flow<List<Chapter>> =
        chapterDao.observeForNovel(novelID).map { list -> list.map { it.toDomain() } }

    /**
     * Returns cached paragraphs when available, otherwise fetch and store.
     **/
    override suspend fun getChapterContent(novelID: Long, chapterID: Long): List<String>? {
        chapterStore.load(novelID, chapterID)?.let { cached ->
            chapterDao.markDownloaded(novelID, chapterID)
            return cached
        }
        val chapter = chapterDao.get(novelID, chapterID) ?: return null
        val content = withContext(ioDispatcher) { client.getChapter(chapter.url) }
        chapterStore.save(novelID, chapterID, content.paragraphs)
        chapterDao.markDownloaded(novelID, chapterID)
        return content.paragraphs
    }

    override suspend fun markChapterRead(novelID: Long, chapterID: Long) {
        chapterDao.markRead(novelID, chapterID)
    }

    override fun observeProgress(novelID: Long): Flow<ReadingProgress?> =
        progressDao.observe(novelID).map { it?.toDomain() }

    override suspend fun saveProgress(progress: ReadingProgress) =
        progressDao.upsert(progress.toEntity())

    override fun observeReaderSettings(novelID: Long): Flow<ReaderSettings> =
        readerSettingsDao.observe(novelID).map { settings ->
            settings?.toDomain() ?: ReaderSettings(novelID = novelID)
        }

    override suspend fun saveReaderSettings(settings: ReaderSettings) {
        readerSettingsDao.upsert(settings.toEntity())
    }

    override suspend fun recordReadingTime(novelID: Long, durationMs: Long) {
        if (durationMs <= 0L) return
        statsDao.insert(
            ReadingSessionEntity(
                novelID = novelID,
                dateEpochDay = LocalDate.now().toEpochDay(),
                durationMs = durationMs,
            )
        )
    }

    override fun observeDailyReadingTime(fromEpochDay: Long): Flow<List<DailyReadingTime>> =
        statsDao.observeDailyReadingTime(fromEpochDay).map { rows ->
            rows.map { row -> DailyReadingTime(day = row.day, totalMs = row.totalMs) }
        }
}

package app.royalreader.data.local.db

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface NovelDao {
    /**
     * Library entries joined with the saved progress chapter, newest first.
     * [LibraryBookRow.resumeChapterID] is null for books never opened.
     **/
    @Query(
        "SELECT n.*, p.chapterID AS resumeChapterID FROM novels n " +
            "LEFT JOIN reading_progress p ON p.novelID = n.id " +
            "WHERE n.inLibrary = 1 ORDER BY n.addedToLibraryAtMs DESC"
    )
    fun observeLibrary(): Flow<List<LibraryBookRow>>

    @Query("SELECT * FROM novels WHERE id = :novelID")
    fun observe(novelID: Long): Flow<NovelEntity?>

    @Query("SELECT * FROM novels WHERE id = :novelID")
    suspend fun get(novelID: Long): NovelEntity?

    @Upsert
    suspend fun upsert(novels: List<NovelEntity>)

    @Query("UPDATE novels SET inLibrary = 1, addedToLibraryAtMs = :timestampMs WHERE id = :novelID")
    suspend fun markInLibrary(novelID: Long, timestampMs: Long)

    @Query("UPDATE novels SET inLibrary = 0, addedToLibraryAtMs = 0 WHERE id = :novelID")
    suspend fun markNotInLibrary(novelID: Long)

    @Query("DELETE FROM novels WHERE id = :novelID")
    suspend fun delete(novelID: Long)
}

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE novelID = :novelID ORDER BY `index` ASC")
    fun observeForNovel(novelID: Long): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE novelID = :novelID ORDER BY `index` ASC")
    suspend fun getForNovel(novelID: Long): List<ChapterEntity>

    @Query("SELECT * FROM chapters WHERE novelID = :novelID AND id = :chapterID")
    suspend fun get(novelID: Long, chapterID: Long): ChapterEntity?

    @Query("SELECT * FROM chapters WHERE novelID = :novelID AND `index` = :indexOf")
    suspend fun getByIndex(novelID: Long, indexOf: Int): ChapterEntity?

    @Query("SELECT * FROM chapters WHERE novelID = :novelID AND `index` = :index + 1")
    suspend fun getNext(novelID: Long, index: Int): ChapterEntity?

    @Query("SELECT * FROM chapters WHERE novelID = :novelID AND `read` = 0 ORDER BY `index` ASC LIMIT 1")
    suspend fun getFirstUnread(novelID: Long): ChapterEntity?

    @Query("SELECT * FROM chapters WHERE novelID = :novelID AND `index` = :index - 1")
    suspend fun getPrevious(novelID: Long, index: Int): ChapterEntity?

    @Query("SELECT * FROM chapters WHERE downloaded = 1 ORDER BY `index` ASC")
    suspend fun getDownloaded(): List<ChapterEntity>

    @Upsert
    suspend fun upsert(chapters: List<ChapterEntity>)

    @Query("UPDATE chapters SET downloaded = 1 WHERE novelID = :novelID AND id = :chapterID")
    suspend fun markDownloaded(novelID: Long, chapterID: Long)

    @Query("UPDATE chapters SET downloaded = 0 WHERE novelID = :novelID")
    suspend fun clearDownloaded(novelID: Long)

    @Query("UPDATE chapters SET `read` = 1 WHERE novelID = :novelID AND id = :chapterID")
    suspend fun markRead(novelID: Long, chapterID: Long)

    @Query("DELETE FROM chapters WHERE novelID = :novelID")
    suspend fun deleteForNovel(novelID: Long)
}

@Dao
interface ProgressDao {
    @Query("SELECT * FROM reading_progress WHERE novelID = :novelID")
    fun observe(novelID: Long): Flow<ReadingProgressEntity?>

    @Query("SELECT * FROM reading_progress WHERE novelID = :novelID")
    suspend fun get(novelID: Long): ReadingProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: ReadingProgressEntity)

    @Query("DELETE FROM reading_progress WHERE novelID = :novelID")
    suspend fun deleteForNovel(novelID: Long)
}

@Dao
interface ReaderSettingsDao {
    @Query("SELECT * FROM reader_settings WHERE novelID = :novelID")
    fun observe(novelID: Long): Flow<ReaderSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: ReaderSettingsEntity)
}

@Dao
interface StatsDao {
    @Query(
        "SELECT dateEpochDay AS day, SUM(durationMs) AS totalMs FROM reading_sessions " +
            "WHERE dateEpochDay >= :fromEpochDay GROUP BY dateEpochDay ORDER BY dateEpochDay ASC"
    )
    fun observeDailyReadingTime(fromEpochDay: Long): Flow<List<DailyReadingTimeRow>>

    @Insert
    suspend fun insert(session: ReadingSessionEntity)
}

data class DailyReadingTimeRow(
    val day: Long,
    val totalMs: Long,
)

/**
 * Projection of a library novel plus its saved progress chapter id, if any.
 **/
data class LibraryBookRow(
    @Embedded val novel: NovelEntity,
    val resumeChapterID: Long?,
)

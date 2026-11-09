package app.royalreader.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room representation of a [app.royalreader.domain.model.Novel].
 * Novels encountered while browsing are cached; [inLibrary] marks what the user added.
 **/
@Entity(tableName = "novels")
data class NovelEntity(
    @PrimaryKey
    val id: Long,
    val title: String,
    val author: String,
    val coverURL: String? = null,
    val description: String = "",
    val tags: List<String> = emptyList(),
    val rating: Float? = null,
    val inLibrary: Boolean = false,
    val addedToLibraryAtMs: Long = 0L,
)

/**
 * Room representation of a [app.royalreader.domain.model.Chapter].
 **/
@Entity(
    tableName = "chapters",
    primaryKeys = ["novelID", "id"],
)
data class ChapterEntity(
    val novelID: Long,
    val id: Long,
    val title: String,
    val index: Int,
    val url: String,
    val publishedAtMs: Long? = null,
    val downloaded: Boolean = false,
    val read: Boolean = false,
)

/**
 * Reading progress of a given novel; one row per novel.
 **/
@Entity(tableName = "reading_progress")
data class ReadingProgressEntity(
    @PrimaryKey
    val novelID: Long,
    val chapterID: Long,
    val paragraphAnchor: Int,
    val updatedAtMs: Long = 0L,
)

/**
 * Per-novel reader settings.
 **/
@Entity(tableName = "reader_settings")
data class ReaderSettingsEntity(
    @PrimaryKey
    val novelID: Long,
    val paletteID: String,
    val fontSizePx: Int,
    val lineHeight: Float,
)

/**
 * Wall-clock time spent in reader screen, per calendar day.
 **/
@Entity(tableName = "reading_sessions")
data class ReadingSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val novelID: Long,
    val dateEpochDay: Long,
    val durationMs: Long,
)

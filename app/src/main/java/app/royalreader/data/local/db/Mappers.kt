package app.royalreader.data.local.db

import app.royalreader.domain.model.Chapter
import app.royalreader.domain.model.DailyReadingTime
import app.royalreader.domain.model.Novel
import app.royalreader.domain.model.ReaderSettings
import app.royalreader.domain.model.ReadingProgress
import java.util.concurrent.TimeUnit

fun NovelEntity.toDomain(): Novel = Novel(
    id = id,
    title = title,
    author = author,
    coverURL = coverURL,
    description = description,
    tags = tags,
    rating = rating,
    inLibrary = inLibrary,
)

fun Novel.toEntity(addedToLibraryAtMs: Long = 0L, inLibrary: Boolean): NovelEntity = NovelEntity(
    id = id,
    title = title,
    author = author,
    coverURL = coverURL,
    description = description,
    tags = tags,
    rating = rating,
    inLibrary = inLibrary,
    addedToLibraryAtMs = addedToLibraryAtMs,
)

fun ChapterEntity.toDomain(): Chapter = Chapter(
    novelID = novelID,
    id = id,
    title = title,
    index = index,
    url = url,
    publishedAtMs = publishedAtMs,
    downloaded = downloaded,
    read = read,
)

fun Chapter.toEntity(): ChapterEntity = ChapterEntity(
    novelID = novelID,
    id = id,
    title = title,
    index = index,
    url = url,
    publishedAtMs = publishedAtMs,
    downloaded = downloaded,
    read = read,
)

fun ReadingProgressEntity.toDomain(): ReadingProgress = ReadingProgress(
    novelID = novelID,
    chapterID = chapterID,
    paragraphAnchor = paragraphAnchor,
    updatedAtMs = updatedAtMs,
)

fun ReadingProgress.toEntity(): ReadingProgressEntity = ReadingProgressEntity(
    novelID = novelID,
    chapterID = chapterID,
    paragraphAnchor = paragraphAnchor,
    updatedAtMs = System.currentTimeMillis(),
)

fun ReaderSettingsEntity.toDomain(): ReaderSettings = ReaderSettings(
    novelID = novelID,
    paletteID = paletteID,
    fontSizePx = fontSizePx,
    lineHeight = lineHeight,
)

fun ReaderSettings.toEntity(): ReaderSettingsEntity = ReaderSettingsEntity(
    novelID = novelID,
    paletteID = paletteID,
    fontSizePx = fontSizePx,
    lineHeight = lineHeight,
)

fun DailyReadingTimeRow.toDomain(): DailyReadingTime = DailyReadingTime(
    day = day,
    totalMs = totalMs,
)

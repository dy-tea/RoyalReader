package app.royalreader.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        NovelEntity::class,
        ChapterEntity::class,
        ReadingProgressEntity::class,
        ReaderSettingsEntity::class,
        ReadingSessionEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(TagListConverter::class)
abstract class RoyalReaderDatabase : RoomDatabase() {
    abstract fun novelDao(): NovelDao
    abstract fun chapterDao(): ChapterDao
    abstract fun progressDao(): ProgressDao
    abstract fun readerSettingsDao(): ReaderSettingsDao
    abstract fun statsDao(): StatsDao

    companion object {
        const val NAME = "royalreader.db"
    }
}

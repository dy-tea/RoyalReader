package app.royalreader.di

import android.content.Context
import androidx.room.Room
import app.royalreader.data.local.db.ChapterDao
import app.royalreader.data.local.db.NovelDao
import app.royalreader.data.local.db.ProgressDao
import app.royalreader.data.local.db.ReaderSettingsDao
import app.royalreader.data.local.db.RoyalReaderDatabase
import app.royalreader.data.local.db.StatsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): RoyalReaderDatabase =
        Room.databaseBuilder(context, RoyalReaderDatabase::class.java, RoyalReaderDatabase.NAME)
            .build()

    @Provides
    fun novelDao(db: RoyalReaderDatabase): NovelDao = db.novelDao()

    @Provides
    fun chapterDao(db: RoyalReaderDatabase): ChapterDao = db.chapterDao()

    @Provides
    fun progressDao(db: RoyalReaderDatabase): ProgressDao = db.progressDao()

    @Provides
    fun readerSettingsDao(db: RoyalReaderDatabase): ReaderSettingsDao = db.readerSettingsDao()

    @Provides
    fun statsDao(db: RoyalReaderDatabase): StatsDao = db.statsDao()
}

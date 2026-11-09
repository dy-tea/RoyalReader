package app.royalreader.di

import app.royalreader.data.local.files.ChapterStore
import app.royalreader.data.remote.RoyalRoadClient
import app.royalreader.data.repository.NovelRepositoryImpl
import app.royalreader.domain.repository.NovelRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    /**
     * Room DAOs are not @Inject-annotated, so the repository is built by hand here.
     **/
    @Provides
    @Singleton
    fun novelRepository(
        client: RoyalRoadClient,
        database: app.royalreader.data.local.db.RoyalReaderDatabase,
        chapterStore: ChapterStore,
    ): NovelRepository = NovelRepositoryImpl(
        client = client,
        novelDao = database.novelDao(),
        chapterDao = database.chapterDao(),
        progressDao = database.progressDao(),
        readerSettingsDao = database.readerSettingsDao(),
        statsDao = database.statsDao(),
        chapterStore = chapterStore,
    )
}

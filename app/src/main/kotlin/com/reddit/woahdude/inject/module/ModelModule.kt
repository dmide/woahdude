package com.reddit.woahdude.inject.module

import android.content.Context
import androidx.room.Room
import com.google.android.exoplayer2.upstream.DataSource
import com.reddit.woahdude.model.RedditDao
import com.reddit.woahdude.model.RedditDb
import com.reddit.woahdude.util.megabytes
import com.reddit.woahdude.video.CacheDataSourceFactory
import dagger.Module
import dagger.Provides
import dagger.Reusable

@Module
@Suppress("unused")
object ModelModule {

    @Provides
    @Reusable
    @JvmStatic
    internal fun provideRedditDb(context: Context): RedditDb {
        return Room.databaseBuilder(context, RedditDb::class.java, "woahdude.db").build()
    }

    @Provides
    @Reusable
    @JvmStatic
    internal fun provideRedditDao(redditDb: RedditDb): RedditDao {
        return redditDb.postDao()
    }

    @Provides
    @Reusable
    @JvmStatic
    internal fun provideDataSourceFactory(context: Context): DataSource.Factory {
        return CacheDataSourceFactory(context, 100.megabytes, 20.megabytes)
    }
}
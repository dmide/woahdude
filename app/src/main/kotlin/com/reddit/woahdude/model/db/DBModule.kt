package com.reddit.woahdude.model.db

import android.content.Context
import androidx.room.Room
import com.google.android.exoplayer2.upstream.DataSource
import com.reddit.woahdude.util.megabytes
import com.reddit.woahdude.video.CacheDataSourceFactory
import dagger.Module
import dagger.Provides
import dagger.Reusable

@Module
@Suppress("unused")
object DBModule {

    @Provides
    @Reusable
    @JvmStatic
    internal fun provideRedditDb(context: Context): RedditDb {
        return Room.databaseBuilder(context, RedditDb::class.java, "woahdude.db").build()
    }
}
package com.reddit.woahdude.inject.module

import android.content.Context
import androidx.room.Room
import com.reddit.woahdude.model.RedditDao
import com.reddit.woahdude.model.RedditDb
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
}
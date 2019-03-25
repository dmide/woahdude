package com.reddit.woahdude.model.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.reddit.woahdude.model.CrossPost
import com.reddit.woahdude.model.RedditPost

@Database(
        entities = [RedditPost::class],
        version = 1,
        exportSchema = false
)
@TypeConverters(CrossPost::class)
internal abstract class RedditDb : RoomDatabase() {

    companion object {
        fun create(context: Context): RedditDb {
            val databaseBuilder = Room.databaseBuilder(context, RedditDb::class.java, "woahdude.db")
            return databaseBuilder
                    .fallbackToDestructiveMigration()
                    .build()
        }
    }

    internal abstract fun postDao(): RedditDao
}

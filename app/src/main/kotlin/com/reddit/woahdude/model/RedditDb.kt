package com.reddit.woahdude.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.reddit.woahdude.network.RedditPost

@Database(
        entities = [RedditPost::class],
        version = 1,
        exportSchema = false
)
abstract class RedditDb : RoomDatabase() {

    companion object {
        fun create(context: Context): RedditDb {
            val databaseBuilder = Room.databaseBuilder(context, RedditDb::class.java, "woahdude.db")
            return databaseBuilder
                    .fallbackToDestructiveMigration()
                    .build()
        }
    }

    abstract fun postDao(): RedditDao
}

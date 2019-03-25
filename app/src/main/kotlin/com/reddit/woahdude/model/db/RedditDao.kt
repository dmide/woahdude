package com.reddit.woahdude.model.db

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.reddit.woahdude.model.RedditPost

@Dao
internal interface RedditDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(redditPosts: List<RedditPost>)

    @Query("SELECT * FROM posts ORDER BY indexInResponse ASC")
    fun posts(): DataSource.Factory<Int, RedditPost>

    @Query("DELETE FROM posts")
    fun deleteAll()

    @Query("SELECT MAX(indexInResponse) + 1 FROM posts")
    fun getNextIndex() : Int
}

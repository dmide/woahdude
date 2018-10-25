package com.reddit.woahdude.model

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.reddit.woahdude.network.RedditPost

@Dao
interface RedditDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(redditPosts: List<RedditPost>)

    @Query("SELECT * FROM posts")
    fun posts(): DataSource.Factory<Int, RedditPost>

    @Query("DELETE FROM posts")
    fun deleteAll()

    @Query("SELECT MAX(indexInResponse) + 1 FROM posts")
    fun getNextIndex() : Int
}

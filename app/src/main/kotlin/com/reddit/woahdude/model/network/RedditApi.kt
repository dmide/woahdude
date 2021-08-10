package com.reddit.woahdude.model.network

import com.reddit.woahdude.model.RedditPost
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RedditApi {

    @GET("/r/{subreddit}/hot.json")
    fun getPosts(@Path("subreddit") subreddit: String,
                 @Query("limit") loadSize: Int = 10,
                 @Query("after") after: String? = null,
                 @Query("before") before: String? = null): Single<PostsResponse>

}

class PostsResponse(val data: PostsListing)

class PostsListing(val children: List<PostsContainer>, val after: String?, val before: String?)

class PostsContainer(val data: RedditPost)


package com.reddit.woahdude.network

import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Query

interface RedditApi {

    @GET("/r/woahdude/hot.json")
    fun getPosts(@Query("limit") loadSize: Int = 30,
                 @Query("after") after: String? = null,
                 @Query("before") before: String? = null): Observable<PostsResponse>

}

class PostsResponse(val data: PostsListing)

class PostsListing(val children: List<PostsContainer>, val after: String?, val before: String?)

class PostsContainer(val data: RedditPost)


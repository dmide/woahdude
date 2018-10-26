package com.reddit.woahdude.network

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import com.reddit.woahdude.common.Const
import com.reddit.woahdude.common.GlideRequest
import com.reddit.woahdude.common.GlideRequests

@Entity(tableName = "posts")
data class RedditPost(
        @PrimaryKey
        @SerializedName("name")
        val name: String,
        @SerializedName("title")
        val title: String,
        @SerializedName("score")
        val score: Int,
        @SerializedName("author")
        val author: String,
        @SerializedName("num_comments")
        val commentsCount: Int,
        @SerializedName("created_utc")
        val created: Long,
        val thumbnail: String?,
        val url: String?) {
    // to be consistent w/ changing backend order, we need to keep a data like this
    var indexInResponse: Int = -1
}

val imageExtensions = arrayOf(".jpg", ".png", ".jpeg", ".gif")

fun RedditPost.loadImage(glide: GlideRequests) : GlideRequest<*> {
    var imageUrl: String? = null
    if (url != null && (imageExtensions.any { url.endsWith(it) })) {
        imageUrl = url
    }

    return glide.load(imageUrl).override(Const.deviceWidth)
}
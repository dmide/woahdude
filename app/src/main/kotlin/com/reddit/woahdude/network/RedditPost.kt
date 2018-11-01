package com.reddit.woahdude.network

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.room.Embedded
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
        @Embedded(prefix = "media")
        val media: Media?,
        val thumbnail: String?,
        @SerializedName("link_flair_text")
        val type: String?,
        val url: String?) {
    // to be consistent w/ changing backend order, we need to keep a data like this
    var indexInResponse: Int = -1
}

fun RedditPost.imageLoadRequest(glide: GlideRequests, imageUrl: String? = getImageUrl()): GlideRequest<Drawable> {
    return glide.load(imageUrl)
            .placeholder(ColorDrawable(Color.TRANSPARENT))
            .override(Const.deviceWidth)
}

private val imageExtensions = arrayOf(".jpg", ".png", ".jpeg", ".gif")

fun RedditPost.getImageUrl(): String? {
    var imageUrl: String? = null
    if (url != null) {
        if (imageExtensions.any { url.endsWith(it) }) {
            imageUrl = url
        } else if (url.endsWith(".gifv")) {
            imageUrl = url.replace(".gifv", "h.jpg") //TODO may break eventually, but fine for an example app
        }
    }
    return imageUrl
}

package com.reddit.woahdude.network

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.widget.ImageView
import androidx.core.view.isVisible
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

fun RedditPost.imageLoadRequest(glide: GlideRequests): GlideRequest<*> {
    return imageLoadRequest(glide, getImageUrl())
}

fun RedditPost.loadImage(glide: GlideRequests, iv: ImageView) {
    val imageUrl = getImageUrl()
    iv.isVisible = imageUrl != null
    imageLoadRequest(glide, imageUrl).into(iv)
}

private val imageExtensions = arrayOf(".jpg", ".png", ".jpeg", ".gif")

private fun RedditPost.getImageUrl(): String? {
    var imageUrl: String? = null
    if (url != null && (imageExtensions.any { url.endsWith(it) })) {
        imageUrl = url
    }
    return imageUrl
}

private fun imageLoadRequest(glide: GlideRequests, imageUrl: String?): GlideRequest<*> {
    return glide.load(imageUrl)
            .placeholder(ColorDrawable(Color.TRANSPARENT))
            .override(Const.deviceWidth)
}

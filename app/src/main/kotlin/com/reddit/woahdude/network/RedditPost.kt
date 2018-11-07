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

//TODO use API instead of this hackery
fun RedditPost.getImageUrl(): String? {
    var imageUrl: String? = null
    if (url != null) {
        if (url.endsWith("giphy.gif")) { // this is not an actual gif but a container
            val imageHash = url.split("/").dropLast(1).last()
            imageUrl = "https://i.giphy.com/$imageHash.gif"
        } else if (url.contains("imgur.com") && url.endsWith(".gifv")) {
            imageUrl = url.replace(".gifv", "h.jpg")
        } else if (url.contains("gfycat.com") && type == "gifv") {
            imageUrl = url.replace("gfycat.com", "thumbs.gfycat.com") + "-poster.jpg"
        } else if (imageExtensions.any { url.endsWith(it) }) {
            imageUrl = url
        }
    }
    return imageUrl
}

fun RedditPost.getPostType(): String? {
    var postType: String? = null
    if (type != null) {
        if (url?.endsWith("giphy.gif") == true) {
            postType = "giphy"
        } else if (url?.contains("v.redd.it/") == true) {
            postType = "redditV"
        } else if (url?.contains("gfycat.com") == true) {
            postType = "gfycat"
        } else {
            postType = type
        }
    }
    return postType
}

//TODO use API instead of this hackery
fun RedditPost.getVideoUrl(): String? {
    var videoUrl: String? = null
    if (url != null) {
        if (url.contains("imgur.com") && url.endsWith(".gifv")) {
            videoUrl = url.replace(".gifv", ".mp4") //TODO may break eventually, but fine for an example app
        } else if (url.contains("v.redd.it/")) {
            val videoHash = url.split("/").last()
            videoUrl = "https://v.redd.it/$videoHash/DASHPlaylist.mpd"
        } else if (url.contains("gfycat.com") && type == "gifv") {
            videoUrl = url.replace("gfycat.com", "giant.gfycat.com") + ".mp4"
        }
    }
    return videoUrl
}
package com.reddit.woahdude.model

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.reddit.woahdude.util.Metrics
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
        @Embedded(prefix = "preview")
        val preview: Preview?,
        @SerializedName("crosspost_parent_list")
        val crosspostParents: ArrayList<CrossPost>?,
        val thumbnail: String?,
        @SerializedName("link_flair_text")
        val type: String?,
        val url: String?,
        val permalink: String?) {
    // to be consistent w/ changing backend order, we need to keep a data like this
    var indexInResponse: Int = -1
    var nextPageToken: String? = null

    fun permalinkUrl(): String {
        return "https://reddit.com$permalink"
    }
}

data class CrossPost(@Embedded(prefix = "media") val media: Media? = null) {
    @TypeConverter
    fun crossPostListToString(crosspostParents: ArrayList<CrossPost>?): String {
        return Gson().toJson(crosspostParents)
    }

    @TypeConverter
    fun stringToCrossPostList(string: String): ArrayList<CrossPost>? {
        val arrayType = object : TypeToken<ArrayList<CrossPost>>() {}.getType()
        return Gson().fromJson(string, arrayType)
    }
}

fun RedditPost.imageLoadRequest(glide: GlideRequests, imageResource: Any? = getImageResource()): GlideRequest<Drawable> {
    return glide.load(imageResource)
            .placeholder(ColorDrawable(Color.TRANSPARENT))
            .override(Metrics.deviceWidth)
}

fun RedditPost.getImageResource(): Any? {
    return MediaProvider.of(this).imageResource()
}

fun RedditPost.getPostType(): String? {
    return MediaProvider.of(this).typeString()
}

fun RedditPost.getVideoUrl(): String? {
    return MediaProvider.of(this).videoUrl()
}

fun RedditPost.shouldShowExternalResButton(): Boolean {
    val res = MediaProvider.of(this)
    return res.shouldShowExternalResButton() || (res.videoUrl() == null && res.imageResource() == null)
}

class Media(
        @Embedded(prefix = "media_video")
        val reddit_video: Video?,
        @Embedded(prefix = "oembed")
        @SerializedName("oembed")
        val embedded: Oembed?)

class Preview(
        @Embedded(prefix = "media_video")
        @SerializedName("reddit_video_preview")
        val reddit_video: Video?)

class Video(
        @SerializedName("fallback_url")
        val fallback_url: String?,
        val height: Int?,
        val width: Int?)

class Oembed(
        @SerializedName("thumbnail_url")
        val thumbnailUrl: String?)
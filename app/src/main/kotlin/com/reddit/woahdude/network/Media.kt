package com.reddit.woahdude.network

import androidx.room.Embedded
import com.google.gson.annotations.SerializedName

class Media(
        @Embedded(prefix = "media_video")
        val reddit_video: RedditVideo?,
        @Embedded(prefix = "oembed")
        @SerializedName("oembed")
        val embedded: Oembed?)

class Preview(
        @Embedded(prefix = "media_video")
        @SerializedName("reddit_video_preview")
        val reddit_video: RedditVideo?)

class RedditVideo(
        @SerializedName("fallback_url")
        val fallback_url: String?,
        val height: Int?,
        val width: Int?)

class Oembed(
        @SerializedName("thumbnail_url")
        val thumbnailUrl: String?)
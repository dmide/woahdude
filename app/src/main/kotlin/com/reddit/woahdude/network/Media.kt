package com.reddit.woahdude.network

import androidx.room.Embedded
import com.google.gson.annotations.SerializedName

class Media(
        @Embedded(prefix = "media_video")
        val reddit_video: RedditVideo?)


class RedditVideo(
        @SerializedName("fallback_url")
        val fallback_url: String?,
        val height: Int?,
        val width: Int?)
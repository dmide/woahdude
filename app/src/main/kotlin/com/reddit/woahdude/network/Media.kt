package com.reddit.woahdude.network

import androidx.room.Embedded
import com.google.gson.annotations.SerializedName

class Media {
    @Embedded(prefix = "media_video")
    var reddit_video: RedditVideo? = null
}

class RedditVideo {
    @SerializedName("fallback_url")
    var fallback_url: String? = null
}
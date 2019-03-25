package com.reddit.woahdude.model

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import com.reddit.woahdude.ui.components.WrappedDrawable
import com.reddit.woahdude.util.Metrics

//TODO use actual APIs instead of this hackery
sealed class MediaProvider {

    companion object {
        fun of(redditPost: RedditPost): MediaProvider {
            val url = redditPost.url
            val type = redditPost.type
            if (url == null) {
                return Default(type, url)
            }
            return when {
                url.endsWith("giphy.gif") -> Giphy(url)
                url.contains("v.redd.it") -> RedditVideo(url, redditPost.media
                        ?: redditPost.crosspostParents?.get(0)?.media)
                url.contains("gfycat.com") && type == "gifv" -> Gfycat(url, redditPost.media
                        ?: redditPost.crosspostParents?.get(0)?.media)
                url.contains("imgur.com") && url.endsWith(".gifv") -> ImgurVideo(redditPost)
                url.contains("youtube.com") || url.contains("youtu.be") -> Youtube(url)
                url.contains("vimeo.com") -> Vimeo(url)
                else -> Default(type, url)
            }
        }

        fun getAdaptedMediaDimensions(w: Int, h: Int): MediaDimensions {
            val widthModifier = Metrics.deviceWidth / w.toFloat()
            val adaptedHeight = Math.min((h * widthModifier).toInt(), Metrics.optimalContentHeight)
            return MediaDimensions(Metrics.deviceWidth, adaptedHeight)
        }
    }

    abstract fun imageResource(): Any?

    abstract fun videoUrl(): String?

    abstract fun typeString(): String?

    open fun shouldShowExternalResButton() = false

    data class MediaDimensions(val width: Int, val height: Int)
}

class Default(private val type: String?, private val url: String?) : MediaProvider() {
    private val imageExtensions = arrayOf(".jpg", ".png", ".jpeg", ".gif")
    private val videoExtensions = arrayOf(".mp4", ".webm")

    override fun imageResource(): String? {
        if (imageExtensions.any { url?.endsWith(it) == true }) {
            return url
        }
        return null
    }

    override fun videoUrl(): String? {
        if (videoExtensions.any { url?.endsWith(it) == true }) {
            return url
        }
        return null
    }

    override fun typeString(): String? {
        return type
    }
}

class Vimeo(private val url: String) : MediaProvider() {
    override fun imageResource(): String? = null

    override fun videoUrl(): String? = null

    override fun typeString(): String? {
        return "vimeo"
    }
}

class Youtube(private val url: String) : MediaProvider() {
    companion object {
        private val fullUrlRegex = "youtube\\.com/watch\\?v=([^&]*)&*".toRegex()
        private val attributionUrlRegex = "v%3D([^&%]*)[%&]*".toRegex()
        private val shortUrlRegex = "youtu\\.be/([^&]*)&*".toRegex()
    }

    override fun imageResource(): String? {
        return "https://img.youtube.com/vi/${extractHash()}/0.jpg"
    }

    override fun videoUrl(): String? = null

    override fun typeString(): String? {
        return "youtube"
    }

    override fun shouldShowExternalResButton() = true

    private fun extractHash() : String? {
        val regex = when {
            url.contains("attribution_link") -> attributionUrlRegex
            url.contains("https://www.youtube.com") -> fullUrlRegex
            url.contains("https://youtu.be/") -> shortUrlRegex
            else -> null
        }

        return regex?.find(url)?.groups?.get(1)?.value ?: ""
    }
}

class ImgurVideo(private val redditPost: RedditPost) : MediaProvider() {
    val url = redditPost.url!! // always non-null at this point

    override fun imageResource(): Any? {
        val height = redditPost.preview?.reddit_video?.height
        val width = redditPost.preview?.reddit_video?.width
        if (height != null && width != null) { //better to get blank preview but with correct dimensions
            val dimensions = getAdaptedMediaDimensions(width, height)
            val drawable = WrappedDrawable(ColorDrawable(Color.TRANSPARENT))
            drawable.setBounds(0, 0, dimensions.width, dimensions.height)
            return drawable
        }
        return url.replace(".gifv", "h.jpg")
    }

    override fun videoUrl(): String? = url.replace(".gifv", ".mp4")

    override fun typeString(): String? = "imgurV"
}

class Giphy(private val url: String) : MediaProvider() {
    override fun imageResource(): String? {
        val imageHash = url.split("/").dropLast(1).last()
        return "https://i.giphy.com/$imageHash.gif"
    }

    override fun videoUrl(): String? = null

    override fun typeString(): String? = "giphy"
}

class RedditVideo(private val url: String, private val media: Media?) : MediaProvider() {
    override fun imageResource(): Any? {
        val height = media?.reddit_video?.height
        val width = media?.reddit_video?.width
        if (height != null && width != null) {
            val dimensions = getAdaptedMediaDimensions(width, height)
            val drawable = WrappedDrawable(ColorDrawable(Color.TRANSPARENT))
            drawable.setBounds(0, 0, dimensions.width, dimensions.height)
            return drawable
        }
        return null
    }

    override fun videoUrl(): String? {
        val videoHash = url.split("/").last()
        return "https://v.redd.it/$videoHash/DASHPlaylist.mpd"
    }

    override fun typeString(): String? = "redditV"
}

class Gfycat(private val url: String, private val media: Media?) : MediaProvider() {
    override fun imageResource(): String? {
        return "https://thumbs.gfycat.com/${getHash()}-poster.jpg"
    }

    override fun videoUrl(): String? {
        return "https://giant.gfycat.com/${getHash()}.mp4"
    }

    override fun typeString(): String? = "gfycat"

    /*
        Some hashes are case-sensitive. thumbnailUrl always contains hash with correct case
        while url is always lowercase.
     */
    private fun getHash(): String {
        media?.embedded?.thumbnailUrl?.let {
            return it.removePrefix("https://thumbs.gfycat.com/").removeSuffix("-size_restricted.gif")
        }
        return url.removePrefix("https://gfycat.com/")
    }
}
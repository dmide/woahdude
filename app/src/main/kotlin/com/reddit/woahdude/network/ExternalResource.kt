package com.reddit.woahdude.network

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import com.reddit.woahdude.ui.components.WrappedDrawable
import com.reddit.woahdude.util.Const

//TODO use actual APIs instead of this hackery
sealed class ExternalResource {

    companion object {
        fun of(redditPost: RedditPost): ExternalResource {
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
                url.contains("imgur.com") && url.endsWith(".gifv") -> ImgurVideo(url)
                url.contains("youtube.com") || url.contains("youtu.be") -> Youtube(url)
                url.contains("vimeo.com") -> Vimeo(url)
                else -> Default(type, url)
            }
        }

        fun getAdaptedMediaDimensions(w: Int, h: Int): MediaDimensions {
            val widthModifier = Const.deviceWidth / w.toFloat()
            val adaptedHeight = Math.min((h * widthModifier).toInt(), Const.optimalContentHeight)
            return MediaDimensions(Const.deviceWidth, adaptedHeight)
        }
    }

    abstract fun imageResource(): Any?

    abstract fun videoUrl(): String?

    abstract fun typeString(): String?

    class Default(private val type: String?, private val url: String?) : ExternalResource() {
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

    class Vimeo(private val url: String) : ExternalResource() {
        override fun imageResource(): String? = null

        override fun videoUrl(): String? = null

        override fun typeString(): String? {
            return "vimeo"
        }
    }

    class Youtube(private val url: String) : ExternalResource() {
        override fun imageResource(): String? = null

        override fun videoUrl(): String? = null

        override fun typeString(): String? {
            return "youtube"
        }
    }

    class ImgurVideo(private val url: String) : ExternalResource() {
        override fun imageResource(): String? = url.replace(".gifv", "h.jpg")

        override fun videoUrl(): String? = url.replace(".gifv", ".mp4")

        override fun typeString(): String? = "imgurV"
    }

    class Giphy(private val url: String) : ExternalResource() {
        override fun imageResource(): String? {
            val imageHash = url.split("/").dropLast(1).last()
            return "https://i.giphy.com/$imageHash.gif"
        }

        override fun videoUrl(): String? = null

        override fun typeString(): String? = "giphy"
    }

    class RedditVideo(private val url: String, private val media: Media?) : ExternalResource() {
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

    class Gfycat(private val url: String, private val media: Media?) : ExternalResource() {
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

    data class MediaDimensions(val width: Int, val height: Int)
}
package com.reddit.woahdude.network

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
                url.contains("v.redd.it") -> RedditVideo(url)
                url.contains("gfycat.com") && type == "gifv" -> Gfycat(url)
                url.contains("imgur.com") && url.endsWith(".gifv") -> ImgurVideo(url)
                url.contains("youtube.com") || url.contains("youtu.be") -> Youtube(url)
                url.contains("vimeo.com") -> Vimeo(url)
                else -> Default(type, url)
            }
        }
    }

    abstract fun imageUrl(): String?

    abstract fun videoUrl(): String?

    abstract fun typeString(): String?

    class Default(private val type: String?, private val url: String?) : ExternalResource() {
        private val imageExtensions = arrayOf(".jpg", ".png", ".jpeg", ".gif")
        private val videoExtensions = arrayOf(".mp4", ".webm")

        override fun imageUrl(): String? {
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
        override fun imageUrl(): String? = null

        override fun videoUrl(): String? = null

        override fun typeString(): String? {
            return "vimeo"
        }
    }

    class Youtube(private val url: String) : ExternalResource() {
        override fun imageUrl(): String? = null

        override fun videoUrl(): String? = null

        override fun typeString(): String? {
            return "youtube"
        }
    }

    class ImgurVideo(private val url: String) : ExternalResource() {
        override fun imageUrl(): String? = url.replace(".gifv", "h.jpg")

        override fun videoUrl(): String? = url.replace(".gifv", ".mp4")

        override fun typeString(): String? = "imgurV"
    }

    class Giphy(private val url: String) : ExternalResource() {
        override fun imageUrl(): String? {
            val imageHash = url.split("/").dropLast(1).last()
            return "https://i.giphy.com/$imageHash.gif"
        }

        override fun videoUrl(): String? = null

        override fun typeString(): String? = "giphy"
    }

    class RedditVideo(private val url: String) : ExternalResource() {
        override fun imageUrl(): String? = null // actual API call needed

        override fun videoUrl(): String? {
            val videoHash = url.split("/").last()
            return "https://v.redd.it/$videoHash/DASHPlaylist.mpd"
        }

        override fun typeString(): String? = "redditV"
    }

    class Gfycat(private val url: String) : ExternalResource() {
        override fun imageUrl(): String? {
            return url.replace("gfycat.com", "thumbs.gfycat.com") + "-poster.jpg"
        }

        override fun videoUrl(): String? {
            return url.replace("gfycat.com", "giant.gfycat.com") + ".mp4"
        }

        override fun typeString(): String? = "gfycat"
    }
}
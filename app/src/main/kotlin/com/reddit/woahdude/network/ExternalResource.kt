package com.reddit.woahdude.network

//TODO use actual APIs instead of this hackery
sealed class ExternalResource {
    companion object {
        fun of(redditPost: RedditPost): ExternalResource {
            redditPost.url?.let { url ->
                if (url.endsWith("giphy.gif")) {
                    return Giphy(url)
                } else if (url.contains("v.redd.it")) {
                    return RedditVideo(url)
                } else if (url.contains("gfycat.com") && redditPost.type == "gifv") {
                    return Gfycat(url)
                } else if (url.contains("imgur.com") && url.endsWith(".gifv")) {
                    return ImgurVideo(url)
                }
            }
            return Default(redditPost.type, redditPost.url)
        }
    }

    abstract fun imageUrl(): String?

    abstract fun videoUrl(): String?

    abstract fun typeString(): String?

    class Default(private val type: String?, private val url: String?) : ExternalResource() {
        private val imageExtensions = arrayOf(".jpg", ".png", ".jpeg", ".gif")

        override fun imageUrl(): String? {
            if (imageExtensions.any { url?.endsWith(it) == true }) {
                return url
            }
            return null
        }

        override fun videoUrl(): String? {
            return null
        }

        override fun typeString(): String? {
            return type
        }
    }

    class ImgurVideo(private val url: String) : ExternalResource() {
        override fun imageUrl(): String? {
            return url.replace(".gifv", "h.jpg")
        }

        override fun videoUrl(): String? {
            return url.replace(".gifv", ".mp4")
        }

        override fun typeString(): String? {
            return "imgurV"
        }
    }

    class Giphy(private val url: String) : ExternalResource() {
        override fun imageUrl(): String? {
            val imageHash = url.split("/").dropLast(1).last()
            return "https://i.giphy.com/$imageHash.gif"
        }

        override fun videoUrl(): String? {
            return null
        }

        override fun typeString(): String? {
            return "giphy"
        }
    }

    class RedditVideo(private val url: String) : ExternalResource() {
        override fun imageUrl(): String? {
            return null // actual API call needed
        }

        override fun videoUrl(): String? {
            val videoHash = url.split("/").last()
            return "https://v.redd.it/$videoHash/DASHPlaylist.mpd"
        }

        override fun typeString(): String? {
            return "redditV"
        }
    }

    class Gfycat(private val url: String) : ExternalResource() {
        override fun imageUrl(): String? {
            return url.replace("gfycat.com", "thumbs.gfycat.com") + "-poster.jpg"
        }

        override fun videoUrl(): String? {
            return url.replace("gfycat.com", "giant.gfycat.com") + ".mp4"
        }

        override fun typeString(): String? {
            return "gfycat"
        }
    }
}
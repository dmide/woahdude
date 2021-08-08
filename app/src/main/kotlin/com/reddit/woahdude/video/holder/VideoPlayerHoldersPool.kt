package com.reddit.woahdude.video.holder

import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class VideoPlayerHoldersPool @Inject constructor(val playerHolderProvider: Provider<VideoPlayerHolder>) {
    private val reserve: MutableList<VideoPlayerHolder> = mutableListOf()
    private val used: MutableList<VideoPlayerHolder> = mutableListOf()
    private var currentPlayer: VideoPlayerHolder? = null

    fun get(): VideoPlayerHolder {
        val playerHolder =
                if (reserve.isNotEmpty()) reserve.removeAt(0)
                else playerHolderProvider.get()
        used.add(playerHolder)
        return playerHolder
    }

    fun putBack(videoPlayerHolder: VideoPlayerHolder) {
        used.remove(videoPlayerHolder)
        reserve.add(videoPlayerHolder)
    }

    fun pauseCurrent() {
        currentPlayer = used.find { it.isPlaying() }
        currentPlayer?.pause()
    }

    fun resumeCurrent() {
        currentPlayer?.resume()
    }

    fun release() {
        currentPlayer = null
        used.apply {
            forEach { it.release() }
            clear()
        }
        reserve.apply {
            forEach { it.release() }
            clear()
        }
    }
}
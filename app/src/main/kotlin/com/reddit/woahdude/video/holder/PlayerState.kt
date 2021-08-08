package com.reddit.woahdude.video.holder

sealed class PlayerState {
    data class Data(
        val mediaSize: MediaSize = MediaSize(0,0),
        val isLoading: Boolean = false,
        val hasSound: Boolean = false,
        val isSoundEnabled: Boolean = false
    ) : PlayerState()

    data class Error(val exception: java.lang.Exception) : PlayerState()
}
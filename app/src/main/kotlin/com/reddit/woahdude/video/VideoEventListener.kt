package com.reddit.woahdude.video

import android.os.SystemClock
import android.util.Log

import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray

import java.text.NumberFormat
import java.util.Locale

internal class VideoEventListener : Player.EventListener {

    private val startTimeMs: Long
    private val sessionTimeString: String
        get() = getTimeString(SystemClock.elapsedRealtime() - startTimeMs)

    init {
        startTimeMs = SystemClock.elapsedRealtime()
    }

    override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {

    }

    override fun onLoadingChanged(isLoading: Boolean) {
        Log.d(javaClass.name, "loading [$isLoading]")
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, state: Int) {
        Log.d(javaClass.name, "state [" + sessionTimeString + ", " + playWhenReady + ", "
                + getStateString(state) + "]")
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        Log.d(javaClass.name, "onRepeatModeChanged")
    }

    override fun onPlayerError(error: ExoPlaybackException?) {
        Log.d(javaClass.name, "onPlayerError", error)
    }

    override fun onPositionDiscontinuity(reason: Int) {
        Log.d(javaClass.name, "positionDiscontinuity")
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {

    }

    override fun onSeekProcessed() {

    }

    companion object {

        private val TIME_FORMAT: NumberFormat

        init {
            TIME_FORMAT = NumberFormat.getInstance(Locale.US)
            TIME_FORMAT.minimumFractionDigits = 2
            TIME_FORMAT.maximumFractionDigits = 2
            TIME_FORMAT.isGroupingUsed = false
        }

        private fun getTimeString(timeMs: Long): String {
            return if (timeMs == C.TIME_UNSET) "?" else TIME_FORMAT.format((timeMs / 1000f).toDouble())
        }

        private fun getStateString(state: Int): String {
            when (state) {
                ExoPlayer.STATE_BUFFERING -> return "B"
                ExoPlayer.STATE_ENDED -> return "E"
                ExoPlayer.STATE_IDLE -> return "I"
                ExoPlayer.STATE_READY -> return "R"
                else -> return "?"
            }
        }
    }
}

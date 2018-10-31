package com.reddit.woahdude.video

import android.app.Activity
import android.net.Uri
import android.os.Handler
import android.util.Log
import android.view.TextureView
import android.widget.ProgressBar
import androidx.core.view.isVisible
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.extractor.ExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.util.EventLogger
import com.google.android.exoplayer2.util.Util

open class VideoPlayerHolder(activity: Activity) {
    private val mainHandler: Handler
    private val extractorsFactory: ExtractorsFactory
    private val dataSourceFactory: DataSource.Factory
    private val player: SimpleExoPlayer

    private var playerListener: ExoPlayer.EventListener? = null
    private var progress: ProgressBar? = null
    private var videoSizeChangeListener: ((width: Int, height: Int) -> Unit)? = null

    init {
        val bandwidthMeter = DefaultBandwidthMeter()
        mainHandler = Handler()

        extractorsFactory = DefaultExtractorsFactory()
        val baseDataSourceFactory = DefaultHttpDataSourceFactory(Util.getUserAgent(activity, "com.reddit.woahdude"), bandwidthMeter)
        dataSourceFactory = DefaultDataSourceFactory(activity, bandwidthMeter, baseDataSourceFactory)
        val videoTrackSelectionFactory = AdaptiveTrackSelection.Factory(bandwidthMeter)
        val trackSelector = DefaultTrackSelector(videoTrackSelectionFactory)
        val loadControl = DefaultLoadControl()

        player = ExoPlayerFactory.newSimpleInstance(activity, trackSelector, loadControl)
        player.addListener(VideoEventListener())
        player.addAnalyticsListener(object : EventLogger(null) {
            override fun onVideoSizeChanged(eventTime: AnalyticsListener.EventTime, width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
                super.onVideoSizeChanged(eventTime, width, height, unappliedRotationDegrees, pixelWidthHeightRatio)
                this@VideoPlayerHolder.onVideoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio)
            }
        })
        player.repeatMode = Player.REPEAT_MODE_ALL

        playerListener = object : ExoPlayer.EventListener {
            override fun onLoadingChanged(isLoading: Boolean) {
                progress?.isVisible = isLoading
            }

            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        if (playWhenReady) progress?.isVisible = false
                    }
                    Player.STATE_IDLE -> {
                        player.playWhenReady = true
                        progress?.isVisible = true
                    }
                    Player.STATE_BUFFERING -> progress?.isVisible = true
                }
            }

            override fun onPlayerError(error: ExoPlaybackException?) {
                Log.e(javaClass.name, "onPlayerError", error)
            }
        }
        player.addListener(playerListener)
    }

    open fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
        videoSizeChangeListener?.invoke(width, height)
    }

    fun videoSizeChangeListener(listener: (width: Int, height: Int) -> Unit) {
        videoSizeChangeListener = listener
    }

    fun release() {
        progress = null
        player.playWhenReady = false
        player.removeListener(playerListener)
        playerListener = null
        player.release()
    }

    fun pause() {
        player.playWhenReady = false
    }

    fun playVideoSource(videoPath: String, positionMs: Long, videoView: TextureView, progress: ProgressBar) {
        this.progress = progress
        player.setVideoTextureView(videoView)

        val uri = Uri.parse(videoPath)

        val videoSource = ExtractorMediaSource(uri,
                dataSourceFactory,
                extractorsFactory,
                mainHandler, null)

        // Prepare the player with the source.
        player.seekTo(positionMs)
        player.prepare(videoSource)
        player.playWhenReady = true
    }
}

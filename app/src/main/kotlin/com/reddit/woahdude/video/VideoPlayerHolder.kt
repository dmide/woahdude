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
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.util.EventLogger
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.upstream.*
import com.reddit.woahdude.util.megabytes
import java.lang.IllegalStateException


open class VideoPlayerHolder(activity: Activity) {
    private val mainHandler: Handler
    private val extractorsFactory: ExtractorsFactory
    private val dataSourceFactory: DataSource.Factory
    private val player: SimpleExoPlayer

    private var playerListener: ExoPlayer.EventListener? = null
    private var progress: ProgressBar? = null
    private var currentVideoPath: String? = null
    private var videoSizeChangeListener: ((width: Int, height: Int) -> Unit)? = null

    init {
        val bandwidthMeter = DefaultBandwidthMeter()
        mainHandler = Handler()

        extractorsFactory = DefaultExtractorsFactory()
        dataSourceFactory = CacheDataSourceFactory(activity, 150.megabytes, 40.megabytes)
        val videoTrackSelectionFactory = AdaptiveTrackSelection.Factory(bandwidthMeter)
        val trackSelector = DefaultTrackSelector(videoTrackSelectionFactory)

        val defaultAllocator = DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE)
        val loadControl = ExtendedPlaybackLoadControl(defaultAllocator)

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
                    Player.STATE_BUFFERING -> {
                        progress?.isVisible = true
                    }
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
        progress?.isVisible = false
    }

    fun resume() {
        if (progress == null) {
            throw IllegalStateException("bind() must be called before resume")
        }
        player.playWhenReady = true
    }

    fun isPlaying() = player.playWhenReady
    
    /*
        bind() should be called after prepareVideoSource() to prevent
        previous videoSource rogue frames from appearing
     */
    fun bind(videoView: TextureView, progress: ProgressBar) {
        this.progress = progress
        player.setVideoTextureView(videoView)
    }
    
    fun unbind() {
        progress?.isVisible = false
        progress = null
        player.setVideoTextureView(null)
    }

    fun prepareVideoSource(videoPath: String) {        
        if (videoPath.equals(currentVideoPath)) {
            return
        }
        
        player.stop()
        currentVideoPath = videoPath
        player.seekTo(0)
        player.prepare(createMediaSource(videoPath))
    }

    private fun createMediaSource(videoPath: String): MediaSource {
        val uri = Uri.parse(videoPath)
        val videoSource: MediaSource
        if (videoPath.endsWith(".mpd")) {
            videoSource = DashMediaSource(uri, dataSourceFactory,
                    DefaultDashChunkSource.Factory(dataSourceFactory), mainHandler, null)
        } else {
            videoSource = ExtractorMediaSource(uri, dataSourceFactory,
                    extractorsFactory, mainHandler, null)
        }
        return videoSource
    }
}

class ExtendedPlaybackLoadControl(defaultAllocator: DefaultAllocator) : DefaultLoadControl(defaultAllocator,
        DEFAULT_MIN_BUFFER_MS,
        DEFAULT_MAX_BUFFER_MS,
        DEFAULT_BUFFER_FOR_PLAYBACK_MS * 2,
        DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS * 2,
        DEFAULT_TARGET_BUFFER_BYTES,
        DEFAULT_PRIORITIZE_TIME_OVER_SIZE_THRESHOLDS)
package com.reddit.woahdude.video

import android.content.Context
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
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection.*
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultAllocator
import com.google.android.exoplayer2.util.EventLogger
import io.reactivex.subjects.BehaviorSubject
import java.lang.Exception
import java.lang.RuntimeException
import javax.inject.Inject

open class VideoPlayerHolder @Inject constructor(val context: Context,
                                                 val dataSourceFactory: DataSource.Factory) {
    private val mainHandler: Handler = Handler()
    private val extractorsFactory: ExtractorsFactory
    private val player: SimpleExoPlayer
    private var playerListener: ExoPlayer.EventListener? = null

    private var progress: ProgressBar? = null
    private var currentVideoPath: String? = null

    var sizeSubject = BehaviorSubject.create<Size>()
        private set
    var errorSubject = BehaviorSubject.create<Exception>()
        private set

    init {
        extractorsFactory = DefaultExtractorsFactory()
        val bandwidthFraction = 3f; // prefer better quality even on not-so-good connections
        val videoTrackSelectionFactory = AdaptiveTrackSelection.Factory(
                0,
                DEFAULT_MAX_DURATION_FOR_QUALITY_DECREASE_MS,
                DEFAULT_MIN_DURATION_TO_RETAIN_AFTER_DISCARD_MS,
                bandwidthFraction)
        val trackSelector = DefaultTrackSelector(videoTrackSelectionFactory)

        val defaultAllocator = DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE)
        val loadControl = ExtendedPlaybackLoadControl(defaultAllocator)

        player = ExoPlayerFactory.newSimpleInstance(context, trackSelector, loadControl)
        player.addListener(VideoEventListener())
        player.addAnalyticsListener(object : EventLogger(null) {
            override fun onVideoSizeChanged(eventTime: AnalyticsListener.EventTime, width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
                super.onVideoSizeChanged(eventTime, width, height, unappliedRotationDegrees, pixelWidthHeightRatio)
                sizeSubject.onNext(Size(width, height))
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
                        progress?.isVisible = true
                    }
                    Player.STATE_BUFFERING -> {
                        progress?.isVisible = true
                    }
                }
            }

            override fun onPlayerError(error: ExoPlaybackException?) {
                progress?.isVisible = false
                errorSubject.onNext(error ?: RuntimeException("unknown player error"))
            }
        }
        player.addListener(playerListener)
    }

    fun release() {
        player.playWhenReady = false
        player.removeListener(playerListener)
        playerListener = null
        unbind()
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

        sizeSubject.onComplete()
        sizeSubject = BehaviorSubject.create()
        errorSubject.onComplete()
        errorSubject = BehaviorSubject.create()
    }

    fun prepareVideoSource(videoPath: String) {
        if (videoPath.equals(currentVideoPath)) {
            return
        }

        player.stop()
        currentVideoPath = videoPath
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

data class Size(val w: Int, val h: Int)

class ExtendedPlaybackLoadControl(defaultAllocator: DefaultAllocator) : DefaultLoadControl(defaultAllocator,
        DEFAULT_MIN_BUFFER_MS,
        DEFAULT_MAX_BUFFER_MS,
        DEFAULT_BUFFER_FOR_PLAYBACK_MS * 2,
        DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS * 2,
        DEFAULT_TARGET_BUFFER_BYTES,
        DEFAULT_PRIORITIZE_TIME_OVER_SIZE_THRESHOLDS)
package com.reddit.woahdude.video

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.view.Surface
import android.view.TextureView
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.extractor.ExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.MediaSourceEventListener
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection.*
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultAllocator
import com.google.android.exoplayer2.util.EventLogger
import com.reddit.woahdude.util.clearSurface
import io.reactivex.subjects.BehaviorSubject
import timber.log.Timber
import java.io.IOException
import java.lang.Exception
import java.lang.RuntimeException
import javax.inject.Inject

open class VideoPlayerHolder @Inject constructor(val context: Context,
                                                 val dataSourceFactory: DataSource.Factory) {
    private val mainHandler: Handler = Handler()
    private val extractorsFactory: ExtractorsFactory
    private val player: SimpleExoPlayer

    private var currentVideoPath: String? = null
    private var layout: AspectRatioFrameLayout? = null

    var sizeSubject = BehaviorSubject.create<Size>()
        private set
    var errorSubject = BehaviorSubject.create<Exception>()
        private set
    var loadingSubject = BehaviorSubject.create<Boolean>()
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
        player.repeatMode = Player.REPEAT_MODE_ALL
        player.addAnalyticsListener(object : EventLogger(null) {
            override fun onVideoSizeChanged(eventTime: AnalyticsListener.EventTime, width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
                super.onVideoSizeChanged(eventTime, width, height, unappliedRotationDegrees, pixelWidthHeightRatio)
                sizeSubject.onNext(Size(width, height))
            }

            override fun onLoadingChanged(eventTime: AnalyticsListener.EventTime?, isLoading: Boolean) {
                loadingSubject.onNext(isLoading)
            }

            override fun onRenderedFirstFrame(eventTime: AnalyticsListener.EventTime?, surface: Surface?) {
                layout?.setBackgroundColor(Color.BLACK)
            }

            override fun onPlayerStateChanged(eventTime: AnalyticsListener.EventTime?, playWhenReady: Boolean, state: Int) {
                when (state) {
                    Player.STATE_READY -> loadingSubject.onNext(false)
                    Player.STATE_IDLE, Player.STATE_BUFFERING -> loadingSubject.onNext(true)
                }
            }

            override fun onDrmSessionManagerError(eventTime: AnalyticsListener.EventTime?, e: Exception?) = onError(e)

            override fun onPlayerError(eventTime: AnalyticsListener.EventTime?, e: ExoPlaybackException?) = onError(e)

            override fun onLoadError(eventTime: AnalyticsListener.EventTime?,
                                     loadEventInfo: MediaSourceEventListener.LoadEventInfo?,
                                     mediaLoadData: MediaSourceEventListener.MediaLoadData?,
                                     e: IOException?, wasCanceled: Boolean) = onError(e)

            override fun logd(msg: String?) = Timber.d(msg)

            override fun loge(msg: String?, tr: Throwable?) = Timber.e(tr,msg)

            private fun onError(error: Exception?) {
                loadingSubject.onNext(false)
                errorSubject.onNext(error ?: RuntimeException("unknown player error"))
            }
        })
    }

    fun pause() {
        player.playWhenReady = false
    }

    fun resume() {
        player.playWhenReady = true
    }

    fun isPlaying() = player.playWhenReady

    /*
        bind() should be called after prepareVideoSource() to prevent
        previous videoSource rogue frames from appearing
     */
    fun bind(videoView: TextureView, layout: AspectRatioFrameLayout) {
        videoView.surfaceTexture?.let { clearSurface(it) }
        player.setVideoTextureView(videoView)
        this.layout = layout
        layout.background = null
    }

    fun unbind() {
        player.stop(true)
        currentVideoPath = null
        loadingSubject.onNext(false)
        player.setVideoTextureView(null)
        layout = null

        listOf(loadingSubject, sizeSubject, errorSubject).forEach { it.onComplete() }
        loadingSubject = BehaviorSubject.create()
        sizeSubject = BehaviorSubject.create()
        errorSubject = BehaviorSubject.create()
    }

    fun prepareVideoSource(videoPath: String) {
        if (videoPath == currentVideoPath) return
        currentVideoPath = videoPath
        player.prepare(createMediaSource(videoPath))
    }

    fun release() {
        unbind()
        player.release()
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
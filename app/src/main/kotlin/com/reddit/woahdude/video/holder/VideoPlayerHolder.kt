package com.reddit.woahdude.video.holder

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
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection.DEFAULT_MAX_DURATION_FOR_QUALITY_DECREASE_MS
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection.DEFAULT_MIN_DURATION_TO_RETAIN_AFTER_DISCARD_MS
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultAllocator
import com.google.android.exoplayer2.util.EventLogger
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class VideoPlayerHolder @Inject constructor(
    context: Context,
    private val dataSourceFactory: DataSource.Factory,
    private val mainHandler: Handler
) {
    private val extractorsFactory: ExtractorsFactory
    private val player: SimpleExoPlayer

    private var currentVideoPath: String? = null
    private var layout: AspectRatioFrameLayout? = null
    private var stateSubject = BehaviorSubject.create<PlayerState>()

    val stateObservable: Observable<PlayerState> get() = stateSubject

    init {
        extractorsFactory = DefaultExtractorsFactory()
        val bandwidthFraction = 3f // prefer better quality even on not-so-good connections
        val videoTrackSelectionFactory = AdaptiveTrackSelection.Factory(
            0,
            DEFAULT_MAX_DURATION_FOR_QUALITY_DECREASE_MS,
            DEFAULT_MIN_DURATION_TO_RETAIN_AFTER_DISCARD_MS,
            bandwidthFraction
        )
        val trackSelector = DefaultTrackSelector(videoTrackSelectionFactory)
        val defaultAllocator = DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE)
        val loadControl = ExtendedPlaybackLoadControl(defaultAllocator)

        player = ExoPlayerFactory.newSimpleInstance(context, trackSelector, loadControl)
        player.repeatMode = Player.REPEAT_MODE_ALL
        player.volume = 0f
        player.addAnalyticsListener(object : EventLogger(null) {
            override fun onVideoSizeChanged(
                eventTime: AnalyticsListener.EventTime,
                width: Int,
                height: Int,
                unappliedRotationDegrees: Int,
                pixelWidthHeightRatio: Float
            ) {
                super.onVideoSizeChanged(
                    eventTime,
                    width,
                    height,
                    unappliedRotationDegrees,
                    pixelWidthHeightRatio
                )
                stateSubject.onNext(getDataState().copy(mediaSize = MediaSize(width, height)))
            }

            override fun onLoadingChanged(
                eventTime: AnalyticsListener.EventTime?,
                isLoading: Boolean
            ) {
                stateSubject.onNext(getDataState().copy(isLoading = isLoading))
            }

            override fun onRenderedFirstFrame(
                eventTime: AnalyticsListener.EventTime?,
                surface: Surface?
            ) {
                layout?.setBackgroundColor(Color.BLACK)
                layout?.alpha = 1f
            }

            override fun onPlayerStateChanged(
                eventTime: AnalyticsListener.EventTime?,
                playWhenReady: Boolean,
                state: Int
            ) {
                when (state) {
                    Player.STATE_READY, Player.STATE_IDLE -> {
                        stateSubject.onNext(getDataState().copy(isLoading = false))
                    }
                    Player.STATE_BUFFERING -> {
                        stateSubject.onNext(getDataState().copy(isLoading = true))
                    }
                }
            }

            override fun onAudioSessionId(
                eventTime: AnalyticsListener.EventTime?,
                audioSessionId: Int
            ) {
                stateSubject.onNext(getDataState().copy(hasSound = true))
            }

            override fun onDrmSessionManagerError(
                eventTime: AnalyticsListener.EventTime?,
                e: Exception?
            ) = onError(e)

            override fun onPlayerError(
                eventTime: AnalyticsListener.EventTime?,
                e: ExoPlaybackException?
            ) = onError(e)

            override fun onLoadError(
                eventTime: AnalyticsListener.EventTime?,
                loadEventInfo: MediaSourceEventListener.LoadEventInfo?,
                mediaLoadData: MediaSourceEventListener.MediaLoadData?,
                e: IOException?, wasCanceled: Boolean
            ) = onError(e)

            override fun logd(msg: String?) = Timber.d(msg)

            override fun loge(msg: String?, tr: Throwable?) = Timber.e(tr, msg)

            private fun onError(error: Exception?) {
                val exception = error ?: RuntimeException("unknown player error")
                stateSubject.onNext(PlayerState.Error(exception))
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

    fun toggleVolume() {
        if (player.volume == 1f) {
            stateSubject.onNext(getDataState().copy(isSoundEnabled = false))
            player.volume = 0f
        } else {
            stateSubject.onNext(getDataState().copy(isSoundEnabled = true))
            player.volume = 1f
        }
    }

    /*
        bind() should be called after prepareVideoSource() to prevent
        previous videoSource rogue frames from appearing
     */
    fun bind(videoView: TextureView, layout: AspectRatioFrameLayout) {
        player.setVideoTextureView(videoView)
        this.layout = layout
        layout.alpha = 0f
        layout.background = null
    }

    fun unbind() {
        player.stop(true)
        currentVideoPath = null
        stateSubject.onNext(getDataState().copy(isLoading = false))
        player.setVideoTextureView(null)
        player.volume = 0f
        layout = null

        stateSubject.onComplete()
        stateSubject = BehaviorSubject.create()
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

        return if (videoPath.endsWith(".mpd")) {
            DashMediaSource(
                uri, dataSourceFactory,
                DefaultDashChunkSource.Factory(dataSourceFactory), mainHandler, null
            )
        } else {
            ExtractorMediaSource(
                uri, dataSourceFactory,
                extractorsFactory, mainHandler, null
            )
        }
    }

    private fun getDataState(): PlayerState.Data {
        val currentState = stateSubject.value
        return if (currentState is PlayerState.Data) {
            currentState
        } else {
            PlayerState.Data()
        }
    }

    private class ExtendedPlaybackLoadControl(defaultAllocator: DefaultAllocator) : DefaultLoadControl(
        defaultAllocator,
        DEFAULT_MIN_BUFFER_MS,
        DEFAULT_MAX_BUFFER_MS,
        DEFAULT_BUFFER_FOR_PLAYBACK_MS * 2,
        DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS * 2,
        DEFAULT_TARGET_BUFFER_BYTES,
        DEFAULT_PRIORITIZE_TIME_OVER_SIZE_THRESHOLDS
    )
}
package com.reddit.woahdude.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.reddit.woahdude.R
import com.reddit.woahdude.databinding.ActivityVideoBinding
import com.reddit.woahdude.util.megabytes
import com.reddit.woahdude.video.CacheDataSourceFactory
import com.reddit.woahdude.video.VideoPlayerHolder

class VideoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoBinding
    private var playerHolder: VideoPlayerHolder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_video)
        binding.videoViewContainer.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
    }

    override fun onPause() {
        playerHolder?.release()
        playerHolder = null
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        initPlayer()
    }

    private fun initPlayer() {
        if (playerHolder != null) {
            return
        }

        val dataSourceFactory = CacheDataSourceFactory(this, 100.megabytes, 20.megabytes)
        playerHolder = VideoPlayerHolder(this@VideoActivity, dataSourceFactory)
                .apply {
                    // will be GCed
                    sizeSubject.subscribe { (w, h) ->
                        binding.videoViewContainer.setAspectRatio(w.toFloat() / h.toFloat())
                    }
                    // will be GCed
                    loadingSubject.map { if (it) View.VISIBLE else View.INVISIBLE }.subscribe {
                        binding.progress.visibility = it
                    }
                    prepareVideoSource("https://i.imgur.com/OBeI8Dy.mp4")
                    bind(binding.videoView)
                    resume()
                }
    }
}
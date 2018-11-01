package com.reddit.woahdude.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.reddit.woahdude.R
import com.reddit.woahdude.databinding.ActivityVideoBinding
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

        playerHolder = object : VideoPlayerHolder(this@VideoActivity) {
            override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
                super.onVideoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio)
                binding.videoViewContainer.setAspectRatio(width.toFloat() / height.toFloat())
            }
        }
        playerHolder?.playVideoSource("https://i.imgur.com/OBeI8Dy.mp4", 0, binding.videoView, binding.progress)
    }
}
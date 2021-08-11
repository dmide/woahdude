package com.reddit.woahdude.ui.feed

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.reddit.woahdude.R
import com.reddit.woahdude.common.GlideApp
import com.reddit.woahdude.databinding.ListItemBinding
import com.reddit.woahdude.model.*
import com.reddit.woahdude.util.*
import com.reddit.woahdude.video.holder.PlayerState
import com.reddit.woahdude.video.holder.VideoPlayerHolder
import com.reddit.woahdude.video.holder.VideoPlayerHoldersPool
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber
import javax.inject.Inject


class PostViewHolder(private val binding: ListItemBinding) : RecyclerView.ViewHolder(binding.root) {
    @Inject
    lateinit var resources: Resources

    @Inject
    lateinit var context: Context

    @Inject
    lateinit var playerHoldersPool: VideoPlayerHoldersPool

    val maxImageHeight = Metrics.optimalContentHeight
    val postTitle: LiveData<String> = MutableLiveData()
    val postType: LiveData<String> = MutableLiveData()
    val postComments: LiveData<String> = MutableLiveData()

    private val controls: List<View> = listOf(binding.title, binding.comments, binding.type, binding.sound)

    private var redditPost: RedditPost? = null
    private var videoPlayerHolder: VideoPlayerHolder? = null
    private var compositeDisposable = CompositeDisposable()
    private var isFullscreen = false

    init {
        binding.videoView.setOnClickListener {
            toggleVolume()
        }
    }

    fun layoutToFullscreen() {
        isFullscreen = true

        binding.container.apply {
            layoutParams.height = Metrics.contentHeight
            setBackgroundColor(Color.BLACK)
        }
        binding.card.apply {
            layoutParams.height = MATCH_PARENT
            (layoutParams as ViewGroup.MarginLayoutParams).setMargins(0, 0, 0, 0)
        }
        controls.forEach {
            if (it is TextView) {
                it.setShadowLayer(18f, 0f, 0f, Color.BLACK)
                it.setTextColor(Color.WHITE)
            }
        }

        binding.sound.setOnClickListener {
            toggleVolume()
        }
        binding.videoView.setOnClickListener(null)
        binding.videoView.isClickable = false
        binding.imageView.isClickable = false
        binding.container.setOnClickListener {
            controls.forEach {
                it.animate()
                    .alpha(if (it.alpha == 1f) 0f else 1f)
                    .setDuration(200L)
                    .start()
            }
        }
    }

    fun bind(redditPost: RedditPost?) {
        if (isFullscreen){
            controls.forEach {
                it.alpha = 0f
            }
        }

        this.redditPost = redditPost

        if (redditPost == null) {
            listOf(postTitle, postType, postComments).forEach { (it as MutableLiveData).value = "" }
            binding.externalLinkButton.isVisible = false
        } else {
            (postTitle as MutableLiveData).value =
                adapterPosition.toString() + ". " + redditPost.title
            (postType as MutableLiveData).value = redditPost.getPostType()
            (postComments as MutableLiveData).value =
                resources.getString(R.string.comments, redditPost.commentsCount)

            loadImage(redditPost)
            loadVideo(redditPost)
        }

        binding.viewHolder = this
    }

    private fun loadImage(redditPost: RedditPost) {
        binding.placeholder.setImageResource(R.drawable.list_placeholder)
        binding.progress.isVisible = true
        redditPost.imageLoadRequest(GlideApp.with(context), redditPost.getImageResource())
            .onFinish({
                binding.progress.isVisible = false
            }, { e ->
                Timber.e(e, "onImageError")
                binding.imageView.isVisible = false
                onError()
            })
            .into(binding.imageView)
    }

    private fun loadVideo(redditPost: RedditPost) {
        binding.videoViewContainer.layoutParams.height = 0 // reset height after previous video

        val videoUrl = redditPost.getVideoUrl() ?: return

        val playerHolder = videoPlayerHolder ?: playerHoldersPool.get()
        videoPlayerHolder = playerHolder
        playerHolder.prepareVideoSource(videoUrl)
        playerHolder.bind(binding.videoView, binding.videoViewContainer)

        compositeDisposable.clear()
        playerHolder.stateObservable.subscribe { state ->
            when (state) {
                is PlayerState.Data -> {
                    binding.videoViewContainer.isVisible = true

                    binding.progress.isVisible = state.isLoading

                    val (w, h) = state.mediaSize
                    val dimensions = MediaProvider.getAdaptedMediaDimensions(w, h)
                    binding.videoViewContainer.layoutParams.height = dimensions.height
                    binding.videoViewContainer.setAspectRatio(w.toFloat() / h.toFloat())

                    binding.sound.isVisible = state.hasSound
                    val soundIcon =
                        if (state.isSoundEnabled) R.drawable.ic_volume_on else R.drawable.ic_volume_off
                    binding.sound.setImageResource(soundIcon)
                }
                is PlayerState.Error -> {
                    Timber.e(state.exception, "onPlayerError")
                    onError()
                }
            }
        }.addTo(compositeDisposable)
    }

    fun release() {
        videoPlayerHolder?.let {
            it.pause()
            it.unbind()
            playerHoldersPool.putBack(it)
        }
        videoPlayerHolder = null
        compositeDisposable.clear()
    }

    fun showVideoIfNeeded() {
        if (redditPost?.getVideoUrl() != null) {
            videoPlayerHolder?.resume()
        }
    }

    fun onCommentsClick() {
        redditPost?.let {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(it.permalinkUrl()))
            (binding.root.context as Activity).startActivity(browserIntent)
        }
    }

    fun onUrlClick() {
        redditPost?.let {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(it.url))
            (binding.root.context as Activity).startActivity(browserIntent)
        }
    }

    fun onTypeClick() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("woahdude link", redditPost?.permalinkUrl())
        clipboard.setPrimaryClip(clip)
        context.toast(resources.getText(R.string.link_copied_to_clipboard))
    }

    fun externalResButtonVisibility(): Int {
        return if (redditPost?.shouldShowExternalResButton() == true) VISIBLE else GONE
    }

    fun imageViewVisibility(): Int {
        return if (redditPost?.getImageResource() != null) VISIBLE else GONE
    }

    fun videoViewVisibility(): Int {
        return if (redditPost?.getVideoUrl() != null) VISIBLE else GONE
    }

    private fun toggleVolume() {
        if (redditPost?.getVideoUrl() != null) {
            videoPlayerHolder?.toggleVolume()
        }
    }

    private fun onError() {
        binding.apply {
            placeholder.setImageResource(R.drawable.list_error_placeholder)
            videoViewContainer.isVisible = false
            progress.isVisible = false
            externalLinkButton.isVisible = true
        }
    }
}

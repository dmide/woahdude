package com.reddit.woahdude.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.reddit.woahdude.R
import com.reddit.woahdude.common.GlideApp
import com.reddit.woahdude.databinding.ListItemBinding
import com.reddit.woahdude.network.*
import com.reddit.woahdude.util.Const
import com.reddit.woahdude.util.onFinish
import com.reddit.woahdude.util.toast
import com.reddit.woahdude.video.VideoPlayerHolder
import com.reddit.woahdude.video.VideoPlayerHoldersPool
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

    val maxImageHeight = Const.optimalContentHeight
    val postTitle: LiveData<String> = MutableLiveData()
    val postType: LiveData<String> = MutableLiveData()
    val postComments: LiveData<String> = MutableLiveData()

    private var redditPost: RedditPost? = null
    private var videoPlayerHolder: VideoPlayerHolder? = null
    private var compositeDisposable: CompositeDisposable? = null

    fun bind(redditPost: RedditPost?) {
        this.redditPost = redditPost

        if (redditPost == null) {
            listOf(postTitle, postType, postComments).forEach { (it as MutableLiveData).value = "" }
            binding.externalLinkButton.isVisible = false
        } else {
            (postTitle as MutableLiveData).value = adapterPosition.toString() + ". " + redditPost.title
            (postType as MutableLiveData).value = redditPost.getPostType()
            (postComments as MutableLiveData).value = resources.getString(R.string.comments, redditPost.commentsCount)

            loadImage(redditPost)
            loadVideo(redditPost)

            binding.externalLinkButton.isVisible = redditPost.getVideoUrl() == null && redditPost.getImageResource() == null
        }

        binding.type.setOnClickListener {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("woahdude link", redditPost?.permalinkUrl())
            clipboard.primaryClip = clip
            context.toast(resources.getText(R.string.link_copied_to_clipboard))
        }

        binding.viewHolder = this
    }

    private fun loadImage(redditPost: RedditPost) {
        val imageResource = redditPost.getImageResource()
        binding.placeholder.setImageResource(R.drawable.list_placeholder)
        binding.imageView.isVisible = imageResource != null

        binding.progress.isVisible = true
        redditPost.imageLoadRequest(GlideApp.with(context), imageResource)
                .onFinish({
                    binding.progress.isVisible = false
                }, { e ->
                    Timber.e(e, "onImageError")
                    onError()
                })
                .into(binding.imageView)
    }

    private fun loadVideo(redditPost: RedditPost) {
        binding.videoViewContainer.layoutParams.height = 0 // reset height after previous video

        val videoUrl = redditPost.getVideoUrl()
        binding.videoViewContainer.isVisible = videoUrl != null
        if (videoUrl == null) {
            return
        }

        videoPlayerHolder = (videoPlayerHolder ?: playerHoldersPool.get()).apply {
            prepareVideoSource(videoUrl)
            bind(binding.videoView, binding.videoViewContainer)

            compositeDisposable = CompositeDisposable().apply {
                val loadingDisposable = loadingSubject.map { if (it) View.VISIBLE else View.GONE }.subscribe {
                    binding.progress.visibility = it
                }
                add(loadingDisposable)
                add(sizeSubject.subscribe { (w, h) ->
                    val dimensions = ExternalResource.getAdaptedMediaDimensions(w, h)
                    binding.videoViewContainer.layoutParams.height = dimensions.height
                    binding.videoViewContainer.setAspectRatio(w.toFloat() / h.toFloat())
                })
                add(errorSubject.subscribe { e ->
                    Timber.e(e, "onPlayerError")
                    compositeDisposable?.remove(loadingDisposable)
                    onError()
                })
            }
        }
    }

    fun release() {
        videoPlayerHolder?.let {
            it.pause()
            it.unbind()
            playerHoldersPool.putBack(it)
        }
        videoPlayerHolder = null
        compositeDisposable?.dispose()
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

    private fun onError() {
        binding.placeholder.setImageResource(R.drawable.list_error_placeholder)
        binding.progress.isVisible = false
        binding.imageView.isVisible = false
        binding.videoViewContainer.isVisible = false
        binding.externalLinkButton.isVisible = true
    }
}

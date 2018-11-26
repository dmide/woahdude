package com.reddit.woahdude.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.util.Log
import androidx.core.view.isVisible
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.reddit.woahdude.R
import com.reddit.woahdude.common.GlideApp
import com.reddit.woahdude.databinding.ListItemBinding
import com.reddit.woahdude.network.*
import com.reddit.woahdude.util.Const
import com.reddit.woahdude.util.onFinish
import com.reddit.woahdude.video.VideoPlayerHolder
import com.reddit.woahdude.video.VideoPlayerHoldersPool
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject


class PostViewHolder(val binding: ListItemBinding) : RecyclerView.ViewHolder(binding.root) {
    @Inject
    lateinit var resources: Resources
    @Inject
    lateinit var context: Context
    @Inject
    lateinit var playerHoldersPool: VideoPlayerHoldersPool

    val maxImageHeight = Const.contentHeight
    val postTitle = MutableLiveData<String>()
    val postType = MutableLiveData<String>()
    val postComments = MutableLiveData<String>()

    var redditPost: RedditPost? = null
    var videoPlayerHolder: VideoPlayerHolder? = null
    var compositeDisposable: CompositeDisposable? = null

    fun bind(redditPost: RedditPost?) {
        this.redditPost = redditPost

        if (redditPost == null) {
            listOf(postTitle, postType, postComments).forEach { it.value = "" }
            binding.externalLinkButton.isVisible = false
        } else {
            postTitle.value = adapterPosition.toString() + ". " + redditPost.title
            postType.value = redditPost.getPostType()
            postComments.value = resources.getString(R.string.comments, redditPost.commentsCount)

            loadImage(redditPost)
            loadVideo(redditPost)

            binding.externalLinkButton.isVisible = redditPost.getVideoUrl() == null && redditPost.getImageResource() == null
        }

        binding.viewHolder = this
    }

    private fun loadImage(redditPost: RedditPost) {
        val imageResource = redditPost.getImageResource()
        binding.imageView.isVisible = imageResource != null

        binding.progress.isVisible = true
        binding.imageView.background = null
        redditPost.imageLoadRequest(GlideApp.with(context), imageResource)
                .onFinish {
                    binding.progress.isVisible = false
                    binding.imageView.background = ColorDrawable(Color.BLACK)
                }
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
            bind(binding.videoView, binding.progress)

            compositeDisposable = CompositeDisposable().apply {
                add(sizeSubject.subscribe { (w, h) ->
                    val dimensions = ExternalResource.getAdaptedMediaDimensions(w, h)
                    binding.videoViewContainer.layoutParams.height = dimensions.height
                    binding.videoViewContainer.setAspectRatio(w.toFloat() / h.toFloat())
                })
                add(errorSubject.subscribe { e ->
                    Log.e(PostViewHolder::javaClass.name, "onPlayerError", e)
                    binding.externalLinkButton.isVisible = true
                })
            }
        }
    }

    fun releaseVideoPlayerHolder() {
        compositeDisposable?.dispose()
        videoPlayerHolder?.let {
            it.pause()
            it.unbind()
            playerHoldersPool.putBack(it)
        }
        videoPlayerHolder = null
    }

    fun showVideoIfNeeded() {
        if (redditPost?.getVideoUrl() == null) {
            return
        }
        videoPlayerHolder?.resume()
    }

    fun onCommentsClick() {
        redditPost?.let {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://reddit.com" + it.permalink))
            (binding.root.context as Activity).startActivity(browserIntent)
        }
    }

    fun onUrlClick() {
        redditPost?.let {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(it.url))
            (binding.root.context as Activity).startActivity(browserIntent)
        }
    }
}
package com.reddit.woahdude.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.util.Log
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
import com.reddit.woahdude.video.VideoPlayerHolder
import com.reddit.woahdude.video.VideoPlayerHoldersPool
import io.reactivex.disposables.CompositeDisposable
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
            bind(binding.videoView)

            compositeDisposable = CompositeDisposable().apply {
                add(loadingSubject.map { if (it) View.VISIBLE else View.GONE }.subscribe {
                    binding.progress.visibility = it
                })
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
package com.reddit.woahdude.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
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

    val postTitle = MutableLiveData<String>()
    val postType = MutableLiveData<String>()
    val postComments = MutableLiveData<String>()
    var redditPost: RedditPost? = null
    var videoPlayerHolder: VideoPlayerHolder? = null
    var compositeDisposable: CompositeDisposable? = null

    fun bind(redditPost: RedditPost?) {
        this.redditPost = redditPost

        if (redditPost == null) {
            postTitle.value = ""
            postType.value = ""
            postComments.value = ""
            binding.externalLinkButton.isVisible = false
        } else {
            val commentCountString = resources.getString(R.string.comments, redditPost.commentsCount)
            postTitle.value = adapterPosition.toString() + ". " + redditPost.title
            postType.value = redditPost.getPostType()
            postComments.value = commentCountString

            val imageResource = redditPost.getImageResource()
            binding.imageView.isVisible = imageResource != null
            binding.videoViewContainer.isVisible = false
            binding.progress.isVisible = true
            redditPost.imageLoadRequest(GlideApp.with(context), imageResource)
                    .onFinish { binding.progress.isVisible = false }
                    .into(binding.imageView)
            binding.externalLinkButton.isVisible = redditPost.getVideoUrl() == null && redditPost.getImageResource() == null

            redditPost.getVideoUrl()?.let { videoUrl ->
                videoPlayerHolder = (videoPlayerHolder ?: playerHoldersPool.get()).apply {
                    prepareVideoSource(videoUrl)
                    bind(binding.videoView, binding.progress)

                    binding.videoViewContainer.isVisible = true
                    binding.videoViewContainer.layoutParams.height = 0 // reset height after previous video

                    compositeDisposable = CompositeDisposable().apply {
                        add(sizeSubject.subscribe { (w, h) ->
                            val widthModifier = Const.deviceWidth / w.toFloat()
                            val height = Math.min((h * widthModifier).toInt(), Const.contentHeight)
                            binding.videoViewContainer.layoutParams.height = height
                            binding.videoViewContainer.setAspectRatio(w.toFloat() / h.toFloat())
                        })
                        add(errorSubject.subscribe { e ->
                            Log.e(javaClass.name, "onPlayerError", e)
                            binding.externalLinkButton.isVisible = true
                        })
                    }
                }
            }
        }

        binding.viewHolder = this
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
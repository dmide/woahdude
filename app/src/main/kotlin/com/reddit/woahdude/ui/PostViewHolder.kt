package com.reddit.woahdude.ui

import android.content.Context
import android.content.res.Resources
import androidx.core.view.isVisible
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.reddit.woahdude.BuildConfig
import com.reddit.woahdude.R
import com.reddit.woahdude.common.Const
import com.reddit.woahdude.common.GlideApp
import com.reddit.woahdude.common.onFinish
import com.reddit.woahdude.databinding.ListItemBinding
import com.reddit.woahdude.network.*
import com.reddit.woahdude.video.VideoPlayerHolder
import javax.inject.Inject

class PostViewHolder(val binding: ListItemBinding) : RecyclerView.ViewHolder(binding.root) {
    @Inject
    lateinit var resources: Resources
    @Inject
    lateinit var context: Context

    val postTitle = MutableLiveData<String>()
    val postType = MutableLiveData<String>()
    val postComments = MutableLiveData<String>()
    var redditPost: RedditPost? = null

    fun bind(redditPost: RedditPost?) {
        this.redditPost = redditPost

        if (redditPost == null) {
            postTitle.value = ""
            postType.value = ""
            postComments.value = ""
        } else {
            val commentCountString = resources.getString(R.string.comments, redditPost.commentsCount)
            postTitle.value = adapterPosition.toString() + ". " + redditPost.title
            postType.value = redditPost.getPostType()
            postComments.value = commentCountString

            val imageUrl = redditPost.getImageUrl()
            binding.imageView.isVisible = imageUrl != null
            binding.videoViewContainer.isVisible = false
            binding.progress.isVisible = true
            redditPost.imageLoadRequest(GlideApp.with(context), imageUrl)
                    .onFinish { binding.progress.isVisible = false }
                    .into(binding.imageView)
        }

        binding.viewHolder = this
    }

    fun showVideoIfNeeded(playerHolder: VideoPlayerHolder?) {
        redditPost?.let { post ->
            val videoUrl = post.getVideoUrl() ?: return

            playerHolder?.let {
                binding.videoViewContainer.isVisible = true
                playerHolder.videoSizeChangeListener { w, h ->
                    val widthModifier = Const.deviceWidth / w.toFloat()
                    binding.videoViewContainer.layoutParams.height = (h * widthModifier).toInt()
                    binding.videoViewContainer.setAspectRatio(w.toFloat() / h.toFloat())
                }
                playerHolder.playVideoSource(videoUrl, 0, binding.videoView, binding.progress)
            }
        }
    }
}
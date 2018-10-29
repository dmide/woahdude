package com.reddit.woahdude.ui

import android.content.Context
import android.content.res.Resources
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.reddit.woahdude.BuildConfig
import com.reddit.woahdude.R
import com.reddit.woahdude.common.GlideApp
import com.reddit.woahdude.databinding.ListItemBinding
import com.reddit.woahdude.network.RedditPost
import com.reddit.woahdude.network.loadImage
import javax.inject.Inject

class PostViewHolder(private val binding: ListItemBinding) : RecyclerView.ViewHolder(binding.root) {
    @Inject
    lateinit var resources: Resources
    @Inject
    lateinit var context: Context

    val postTitle = MutableLiveData<String>()
    val postComments = MutableLiveData<String>()

    fun bind(redditPost: RedditPost?) {
        if (redditPost == null) {
            postTitle.value = ""
            postComments.value = ""
        } else {
            val commentCountString = resources.getString(R.string.comments, redditPost.commentsCount)
            var title = redditPost.title
            if (BuildConfig.DEBUG) {
                title += " " + redditPost.indexInResponse
            }
            postTitle.value = title
            postComments.value = commentCountString

            redditPost.loadImage(GlideApp.with(context), binding.imageView)
        }

        binding.viewHolder = this
    }
}
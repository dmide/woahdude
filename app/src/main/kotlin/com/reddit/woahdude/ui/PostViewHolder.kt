package com.reddit.woahdude.ui

import android.content.res.Resources
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.reddit.woahdude.R
import com.reddit.woahdude.databinding.ListItemBinding
import com.reddit.woahdude.network.RedditPost
import com.squareup.picasso.Picasso
import javax.inject.Inject

class PostViewHolder(private val binding: ListItemBinding) : RecyclerView.ViewHolder(binding.root) {
    @Inject
    lateinit var resources: Resources

    val imageExtensions = listOf(".jpg", ".png", ".jpeg", ",gif")
    val postTitle = MutableLiveData<String>()
    val postComments = MutableLiveData<String>()

    fun bind(redditPost: RedditPost?) {
        if (redditPost == null) {
            postTitle.value = ""
            postComments.value = ""
        } else {
            val commentCountString = resources.getString(R.string.comments, redditPost.commentsCount)
            postTitle.value = redditPost.title
            postComments.value = commentCountString

            var imageUrl: String? = null
            if (redditPost.url != null && (imageExtensions.any { redditPost.url.endsWith(it) })){
                imageUrl = redditPost.url
            }
            Picasso.with(itemView.context)
                    .load(imageUrl)
                    .into(binding.imageView)
        }

        binding.viewHolder = this
    }
}
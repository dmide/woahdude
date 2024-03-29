package com.reddit.woahdude.ui.feed

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import com.reddit.woahdude.R
import com.reddit.woahdude.app.WDApplication
import com.reddit.woahdude.databinding.ListItemBinding
import com.reddit.woahdude.model.RedditPost

open class ListAdapter : PagedListAdapter<RedditPost, PostViewHolder>(ListDiffUtilCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding: ListItemBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.list_item, parent, false)
        val postViewHolder = PostViewHolder(binding)
        (parent.context.applicationContext as WDApplication).component.inject(postViewHolder)
        return postViewHolder
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: PostViewHolder) {
        holder.release()
        super.onViewRecycled(holder)
    }

    public override fun getItem(position: Int): RedditPost? {
        return super.getItem(position)
    }

    class ListDiffUtilCallback : DiffUtil.ItemCallback<RedditPost>() {

        override fun areItemsTheSame(oldItem: RedditPost, newItem: RedditPost): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: RedditPost, newItem: RedditPost): Boolean {
            return oldItem.name == newItem.name
        }
    }
}

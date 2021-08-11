package com.reddit.woahdude.ui.feed

import android.view.ViewGroup

class PagerAdapter: ListAdapter() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val viewHolder = super.onCreateViewHolder(parent, viewType)
        viewHolder.layoutToFullscreen()
        return viewHolder
    }
}
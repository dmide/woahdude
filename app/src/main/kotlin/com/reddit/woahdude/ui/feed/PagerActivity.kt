package com.reddit.woahdude.ui.feed

import android.os.Bundle
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_UP
import androidx.core.view.doOnNextLayout
import androidx.core.view.get
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_IDLE
import com.reddit.woahdude.R
import com.reddit.woahdude.databinding.ActivityPagerBinding
import com.reddit.woahdude.util.clearAndPostDelayed

class PagerActivity : FeedActivity() {
    companion object {
        private const val HIDE_DELAY_MS = 2500L
    }

    private lateinit var binding: ActivityPagerBinding

    private val toolbarHeight by lazy {
        resources.getDimension(R.dimen.appbar_height)
    }
    private val listAdapter = PagerAdapter()
    private val hideToolbar = Runnable { binding.toolbar.animate().translationY(-toolbarHeight).start() }
    private val showToolbar = Runnable { binding.toolbar.animate().translationY(0f).start() }
    private val hideFab = Runnable { binding.fab.hide() }
    private val showFab = Runnable { binding.fab.show() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_pager)
        binding.lifecycleOwner = this

        binding.toolbar.translationY = -toolbarHeight

        binding.postPager.adapter = listAdapter
        binding.postPager.registerOnPageChangeCallback(PageChangeListener())

        setupSwipeRefreshLayout(binding.swipeRefreshLayout)
        setupToolbar(binding.toolbar)

        binding.fab.apply {
            setOnClickListener {
                binding.postPager.currentItem = 0
                hide(false)
            }
            hide(false)
        }

        viewModel.posts.observe(this, Observer { posts ->
            listAdapter.submitList(posts)
            if (viewModel.lastViewedPosition != -1) {
                binding.postPager.setCurrentItem(viewModel.lastViewedPosition, false)
                viewModel.lastViewedPosition = -1
            }
        })
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == ACTION_UP && ev.y < toolbarHeight && binding.toolbar.translationY != 0f) {
            showToolbar.run()
            viewModel.handler.clearAndPostDelayed(HIDE_DELAY_MS, hideToolbar)
            return true
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun getRootView() = binding.root

    override fun setTitle(title: String) {
        binding.toolbar.title = title
    }

    // https://stackoverflow.com/a/62113146/2093236
    private fun getRecyclerView() = binding.postPager[0] as RecyclerView

    private inner class PageChangeListener : ViewPager2.OnPageChangeCallback() {
        private var isAutoPlayHandled = false

        override fun onPageScrollStateChanged(state: Int) {
            if (state == SCROLL_STATE_IDLE && !isAutoPlayHandled) {
                tryAutoplay()
            }
        }

        override fun onPageSelected(position: Int) {
            if (position > currentPosition) {
                hideFab.run()
                hideToolbar.run()
            } else {
                if (position != 0) showFab.run()
                showToolbar.run()
                viewModel.handler.clearAndPostDelayed(HIDE_DELAY_MS, hideToolbar, hideFab)
            }
            currentPosition = position

            viewModel.playerHoldersPool.pauseCurrent()
            tryAutoplay()
            if (!isAutoPlayHandled) {
                binding.postPager.doOnNextLayout {
                    tryAutoplay()
                }
            }
        }

        private fun tryAutoplay() {
            val holder = getRecyclerView().findViewHolderForAdapterPosition(currentPosition)
            if (holder != null) {
                (holder as PostViewHolder).showVideoIfNeeded()
                isAutoPlayHandled = true
            }
            isAutoPlayHandled = false
        }
    }
}
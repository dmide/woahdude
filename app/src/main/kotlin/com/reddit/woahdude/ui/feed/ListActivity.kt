package com.reddit.woahdude.ui.feed

import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.ListPreloader
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.ViewPreloadSizeProvider
import com.reddit.woahdude.R
import com.reddit.woahdude.common.GlideApp
import com.reddit.woahdude.databinding.ActivityListBinding
import com.reddit.woahdude.model.RedditPost
import com.reddit.woahdude.model.imageLoadRequest
import com.reddit.woahdude.util.weightChildVisibility
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber


class ListActivity : FeedActivity() {
    private lateinit var binding: ActivityListBinding

    private val listAdapter = ListAdapter()
    private val visibleStateSubject = PublishSubject.create<VisibleState>()
    private val onScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val llm = recyclerView.layoutManager as LinearLayoutManager
            visibleStateSubject.onNext(
                VisibleState(llm.findFirstVisibleItemPosition(),
                    llm.findLastVisibleItemPosition())
            )
        }
    }

    private var visibleViewsDisposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_list)
        binding.lifecycleOwner = this

        binding.postList.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            adapter = listAdapter
            addOnScrollListener(setupRecyclerViewPreloader(listAdapter))
            addOnScrollListener(onScrollListener)
            visibleViewsDisposable = setupVisibleViewsObserver(this)
        }

        setupSwipeRefreshLayout(binding.swipeRefreshLayout)
        setupToolbar(binding.toolbar)

        binding.fab.apply {
            setOnClickListener {
                binding.postList.scrollToPosition(0)
                hide(false)
            }
            attachToRecyclerView(binding.postList)
            hide(false)
        }

        viewModel.posts.observe(this, Observer { posts ->
            listAdapter.submitList(posts)
            if (viewModel.lastViewedPosition != -1) {
                binding.postList.scrollToPosition(viewModel.lastViewedPosition)
                viewModel.lastViewedPosition = -1
            } else {
                visibleViewsDisposable?.dispose()
                visibleViewsDisposable = setupVisibleViewsObserver(binding.postList)
            }
        })
    }

    override fun onDestroy() {
        visibleViewsDisposable?.dispose()
        if (!isVideoPoolCleared) {
            viewModel.playerHoldersPool.clear()
        }
        super.onDestroy()
    }

    override fun setTitle(title: String) {
        binding.toolbar.title = title
    }

    override fun getRootView() = binding.root

    private fun setupRecyclerViewPreloader(listAdapter: ListAdapter): RecyclerViewPreloader<RedditPost> {
        val preloadModelProvider = object : ListPreloader.PreloadModelProvider<RedditPost> {
            override fun getPreloadItems(position: Int): MutableList<RedditPost> {
                val item = listAdapter.getItem(position)
                return if (item == null) mutableListOf() else mutableListOf(item)
            }

            override fun getPreloadRequestBuilder(redditPost: RedditPost): RequestBuilder<*> {
                return redditPost.imageLoadRequest(GlideApp.with(this@ListActivity))
            }
        }

        val sizeProvider = ViewPreloadSizeProvider<RedditPost>()
        return RecyclerViewPreloader(Glide.with(this), preloadModelProvider, sizeProvider, 9 /*maxPreload*/)
    }

    private fun setupVisibleViewsObserver(recyclerView: RecyclerView): Disposable {
        val llm = recyclerView.layoutManager as LinearLayoutManager

        return visibleStateSubject
            .switchMap { state ->
                val view = (state.firstVisibleItem..state.lastVisibleItem)
                    .mapNotNull { index -> llm.findViewByPosition(index) }
                    .maxBy { child -> recyclerView.weightChildVisibility(child) }

                return@switchMap if (view != null) Observable.just(view) else Observable.empty()
            }
            .distinctUntilChanged()
            .doOnNext { currentPosition = binding.postList.getChildAdapterPosition(it) }
            .subscribe(
                { mostVisibleChild ->
                    viewModel.playerHoldersPool.pauseCurrent() // pause playback when focus changes
                    val holder = recyclerView.findContainingViewHolder(mostVisibleChild)
                    (holder as PostViewHolder).showVideoIfNeeded()
                },
                { Timber.e(it, "error while observing visible items") });
    }

    private data class VisibleState(val firstVisibleItem: Int, val lastVisibleItem: Int)
}

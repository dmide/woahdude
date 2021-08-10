package com.reddit.woahdude.ui.list

import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.ListPreloader
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.ViewPreloadSizeProvider
import com.google.android.material.snackbar.Snackbar
import com.reddit.woahdude.R
import com.reddit.woahdude.app.WDApplication
import com.reddit.woahdude.common.GlideApp
import com.reddit.woahdude.databinding.ActivityListBinding
import com.reddit.woahdude.model.RedditPost
import com.reddit.woahdude.model.imageLoadRequest
import com.reddit.woahdude.ui.common.BaseActivity
import com.reddit.woahdude.ui.common.ViewModelFactory
import com.reddit.woahdude.ui.settings.SettingsActivity
import com.reddit.woahdude.util.weightChildVisibility
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber


class ListActivity : BaseActivity() {
    private lateinit var binding: ActivityListBinding
    private lateinit var viewModel: ListViewModel

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
    private var snackbar: Snackbar? = null
    private var currentPosition: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val component = (application as WDApplication).component
        viewModel = ViewModelProviders.of(this, ViewModelFactory(component)).get(ListViewModel::class.java)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_list)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        binding.postList.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            adapter = listAdapter
            addOnScrollListener(setupRecyclerViewPreloader(listAdapter))
            addOnScrollListener(onScrollListener)
            visibleViewsDisposable = setupVisibleViewsObserver(this)
        }

        binding.swipeRefreshLayout.apply {
            setOnRefreshListener { viewModel.refreshPosts() }
            val showProgress = Runnable { isRefreshing = true }
            viewModel.loadingVisibility.observe(this@ListActivity, Observer { isLoading ->
                if (!isLoading) {
                    removeCallbacks(showProgress)
                    isRefreshing = false
                } else {
                    postDelayed(showProgress, 1000) //show progress only after 1 second of loading
                }
            })
        }

        binding.fab.apply {
            setOnClickListener {
                binding.postList.scrollToPosition(0)
                hide(false)
            }
            attachToRecyclerView(binding.postList)
            hide(false)
        }

        binding.toolbar.apply {
            title = viewModel.getTitle()
            inflateMenu(R.menu.menu_main)
            setOnMenuItemClickListener { item ->
                if (item.itemId == R.id.settings) {
                    startActivityForResult(
                        Intent(this@ListActivity, SettingsActivity::class.java),
                        SettingsActivity.SETTINGS_REQUEST_CODE
                    )
                }
                return@setOnMenuItemClickListener true
            }
        }

        viewModel.refreshMessage.observe(this, Observer { message ->
            if (message != null) showRefreshSnack(message) else hideRefreshSnack()
        })

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

    override fun onPause() {
        viewModel.playerHoldersPool.pauseCurrent()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        viewModel.playerHoldersPool.resumeCurrent()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SettingsActivity.SETTINGS_REQUEST_CODE) {
            when(resultCode) {
                SettingsActivity.SETTINGS_RESULT_REFRESH_NEEDED -> {
                    viewModel.refreshPosts()
                    binding.toolbar.title = viewModel.getTitle()
                }
            }
        }
    }

    override fun onStop() {
        viewModel.lastViewedPosition = currentPosition
        super.onStop()
    }

    override fun onDestroy() {
        viewModel.playerHoldersPool.release()
        visibleViewsDisposable?.dispose()
        super.onDestroy()
    }

    private fun showRefreshSnack(message: ListViewModel.RefreshMessage) {
        hideRefreshSnack()
        snackbar = Snackbar.make(binding.root, message.text, Snackbar.LENGTH_INDEFINITE)
                .setAction(message.actionText) { viewModel.refreshPosts() }
                .apply { show() }
    }

    private fun hideRefreshSnack() {
        snackbar?.dismiss()
    }

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

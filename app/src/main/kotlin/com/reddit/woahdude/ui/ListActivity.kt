package com.reddit.woahdude.ui

import android.os.Bundle
import android.util.Log
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
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
import com.reddit.woahdude.common.*
import com.reddit.woahdude.databinding.ActivityListBinding
import com.reddit.woahdude.network.RedditPost
import com.reddit.woahdude.network.imageLoadRequest
import com.reddit.woahdude.video.VideoPlayerHolder
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit


class ListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityListBinding
    private lateinit var viewModel: ListViewModel
    private lateinit var visibleViewsDisposable: Disposable
    private val listAdapter: ListAdapter = ListAdapter()
    private var errorSnackbar: Snackbar? = null
    var playerHolder: VideoPlayerHolder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Const.calcDeviceMetrics(this)

        val component = (application as WDApplication).component
        viewModel = ViewModelProviders.of(this, ViewModelFactory(component)).get(ListViewModel::class.java)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_list)
        binding.postList.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            adapter = listAdapter
            addOnScrollListener(setupRecyclerViewPreloader(listAdapter))
            visibleViewsDisposable = setupVisibleViewsObserver(this, layoutManager as LinearLayoutManager)
        }
        binding.swipeRefreshLayout.let { srl ->
            srl.setOnRefreshListener { viewModel.refresh() }
            viewModel.loadingVisibility.observe(this, Observer { srl.isRefreshing = false })
        }
        binding.setLifecycleOwner(this)
        binding.viewModel = viewModel

        viewModel.errorMessage.observe(this, Observer { errorMessage ->
            if (errorMessage != null) showError(errorMessage) else hideError()
        })
        viewModel.posts.observe(this, Observer { posts ->
            listAdapter.submitList(posts)
        })
    }

    override fun onPause() {
        playerHolder?.release()
        playerHolder = null
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (playerHolder != null) {
            return
        }
        playerHolder = VideoPlayerHolder(this)
    }

    override fun onDestroy() {
        visibleViewsDisposable.dispose()
        super.onDestroy()
    }

    private fun showError(@StringRes errorMessage: Int) {
        errorSnackbar = Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_INDEFINITE)
        errorSnackbar?.setAction(R.string.retry) { viewModel.refresh() }
        errorSnackbar?.show()
    }

    private fun hideError() {
        errorSnackbar?.dismiss()
    }

    private fun setupRecyclerViewPreloader(listAdapter: ListAdapter): RecyclerViewPreloader<RedditPost> {
        val preloadModelProvider = object : ListPreloader.PreloadModelProvider<RedditPost> {
            override fun getPreloadItems(position: Int): MutableList<RedditPost> {
                val item = listAdapter.getItem(position)
                return if (item == null) mutableListOf() else mutableListOf(item)
            }

            override fun getPreloadRequestBuilder(redditPost: RedditPost): RequestBuilder<*>? {
                return redditPost.imageLoadRequest(GlideApp.with(this@ListActivity))
            }
        }

        val sizeProvider = ViewPreloadSizeProvider<RedditPost>()
        val preloader = RecyclerViewPreloader(Glide.with(this), preloadModelProvider, sizeProvider, 30 /*maxPreload*/)
        return preloader
    }

    private fun setupVisibleViewsObserver(recyclerView: RecyclerView, llm: LinearLayoutManager): Disposable {
        val publishSubject = PublishSubject.create<VisibleState>()
        val disposable = publishSubject
                .distinctUntilChanged()
                .throttleWithTimeout(250, TimeUnit.MILLISECONDS)
                .map { state ->
                    (state.firstVisibleItem..state.lastVisibleItem)
                            .map { index -> llm.findViewByPosition(index) }
                            .maxBy { child -> recyclerView.getChildVisiblePercent(child) }
                }
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { mostVisibleChild ->
                            if (mostVisibleChild != null) {
                                playerHolder?.pause()
                                val holder = recyclerView.findContainingViewHolder(mostVisibleChild)
                                (holder as PostViewHolder).showVideoIfNeeded(playerHolder)
                            }
                        },
                        { Log.e(javaClass.name, "error while observing visible items", it) })

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                publishSubject.onNext(VisibleState(llm.findFirstVisibleItemPosition(),
                        llm.findLastVisibleItemPosition()))
            }
        })

        return disposable;
    }

    data class VisibleState(val firstVisibleItem: Int, val lastVisibleItem: Int)
}

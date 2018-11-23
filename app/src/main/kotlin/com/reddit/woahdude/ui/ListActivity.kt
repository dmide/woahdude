package com.reddit.woahdude.ui

import android.os.Bundle
import android.util.Log
import android.view.View
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
import com.reddit.woahdude.util.Const
import com.reddit.woahdude.util.weightChildVisibility
import com.reddit.woahdude.video.VideoPlayerHoldersPool
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class ListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityListBinding
    private lateinit var viewModel: ListViewModel
    private lateinit var visibleViewsDisposable: Disposable
    private val listAdapter = ListAdapter()
    private val visibleStatePublishSubject = PublishSubject.create<VisibleState>()
    private var snackbar: Snackbar? = null

    @Inject
    lateinit var playerHoldersPool: VideoPlayerHoldersPool

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Const.calcDeviceMetrics(this)

        val component = (application as WDApplication).component
        component.inject(this)

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
            val showProgress = Runnable { srl.isRefreshing = true }
            viewModel.loadingVisibility.observe(this, Observer { isLoading ->
                if (!isLoading) {
                    srl.removeCallbacks(showProgress)
                    srl.isRefreshing = false
                } else {
                    srl.postDelayed(showProgress, 1000) //show progress only after 1 second of loading
                }
            })
        }
        binding.setLifecycleOwner(this)
        binding.viewModel = viewModel

        viewModel.refreshMessage.observe(this, Observer { message ->
            if (message != null) showRefreshSnack(message) else hideRefreshSnack()
        })
        viewModel.posts.observe(this, Observer { posts ->
            listAdapter.submitList(posts)
        })
    }

    override fun onPause() {
        playerHoldersPool.pauseCurrent()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        playerHoldersPool.resumeCurrent()
    }

    override fun onDestroy() {
        playerHoldersPool.release()
        visibleViewsDisposable.dispose()
        super.onDestroy()
    }

    private fun showRefreshSnack(message: ListViewModel.RefreshMessage) {
        hideRefreshSnack()
        snackbar = Snackbar.make(binding.root, message.text, Snackbar.LENGTH_INDEFINITE)
                .setAction(message.actionText) { viewModel.refresh() }
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

            override fun getPreloadRequestBuilder(redditPost: RedditPost): RequestBuilder<*>? {
                return redditPost.imageLoadRequest(GlideApp.with(this@ListActivity))
            }
        }

        val sizeProvider = ViewPreloadSizeProvider<RedditPost>()
        val preloader = RecyclerViewPreloader(Glide.with(this), preloadModelProvider, sizeProvider, 9 /*maxPreload*/)
        return preloader
    }

    private fun setupVisibleViewsObserver(recyclerView: RecyclerView, llm: LinearLayoutManager): Disposable {
        val disposable = visibleStatePublishSubject
                .throttleWithTimeout(250, TimeUnit.MILLISECONDS)
                // flatmap here is to hack around nulls
                .flatMap<View> { state ->
                    val view = (state.firstVisibleItem..state.lastVisibleItem)
                            .mapNotNull { index -> llm.findViewByPosition(index) }
                            .maxBy { child -> recyclerView.weightChildVisibility(child) }

                    return@flatMap if (view != null) Observable.just(view) else Observable.empty()
                }
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { mostVisibleChild ->
                            playerHoldersPool.pauseCurrent() // pause playback when the focus changes
                            val holder = recyclerView.findContainingViewHolder(mostVisibleChild)
                            (holder as PostViewHolder).showVideoIfNeeded()
                        },
                        { Log.e(javaClass.name, "error while observing visible items", it) })

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                visibleStatePublishSubject.onNext(VisibleState(llm.findFirstVisibleItemPosition(),
                        llm.findLastVisibleItemPosition()))
            }
        })

        return disposable;
    }

    data class VisibleState(val firstVisibleItem: Int, val lastVisibleItem: Int)
}

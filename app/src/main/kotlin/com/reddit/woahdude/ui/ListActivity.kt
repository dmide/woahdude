package com.reddit.woahdude.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
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
import com.reddit.woahdude.common.GlideApp
import com.reddit.woahdude.ui.common.ViewModelFactory
import com.reddit.woahdude.common.WDApplication
import com.reddit.woahdude.databinding.ActivityListBinding
import com.reddit.woahdude.model.RedditPost
import com.reddit.woahdude.model.imageLoadRequest
import com.reddit.woahdude.util.Metrics
import com.reddit.woahdude.util.bindSharedPreference
import com.reddit.woahdude.util.weightChildVisibility
import com.reddit.woahdude.video.VideoPlayerHoldersPool
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import javax.inject.Inject


private const val LAST_VIEWED_POSITION = "LAST_VIEWED_POSITION"

class ListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityListBinding
    private lateinit var viewModel: ListViewModel
    private lateinit var visibleViewsDisposable: Disposable
    private val listAdapter = ListAdapter()
    private val visibleStatePublishSubject = PublishSubject.create<VisibleState>()
    private var snackbar: Snackbar? = null
    private var lastViewedPosition by bindSharedPreference(this, LAST_VIEWED_POSITION, 0)
    private var currentPosition: Int = 0

    @Inject
    lateinit var playerHoldersPool: VideoPlayerHoldersPool

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Metrics.calcDeviceMetrics(this)

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
            srl.setOnRefreshListener { viewModel.refreshPosts() }
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

        binding.fab.apply {
            setOnClickListener {
                binding.postList.scrollToPosition(0)
                hide(false)
            }
            attachToRecyclerView(binding.postList)
            hide(false)
        }

        binding.setLifecycleOwner(this)
        binding.viewModel = viewModel

        viewModel.refreshMessage.observe(this, Observer { message ->
            if (message != null) showRefreshSnack(message) else hideRefreshSnack()
        })
        viewModel.posts.observe(this, Observer { posts ->
            listAdapter.submitList(posts)
            if (lastViewedPosition != -1) {
                binding.postList.scrollToPosition(lastViewedPosition)
                lastViewedPosition = -1
            }
        })

        val toolbar: Toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    override fun onPause() {
        playerHoldersPool.pauseCurrent()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        playerHoldersPool.resumeCurrent()
    }

    override fun onCreateOptionsMenu(menu: Menu) : Boolean {
        menuInflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.getItemId() == R.id.privacy_policy) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.privacy_policy_url)));
            startActivity(browserIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    override fun onDestroy() {
        playerHoldersPool.release()
        visibleViewsDisposable.dispose()
        if (isFinishing) {
            lastViewedPosition = currentPosition
        }
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
                // switchMap here is to hack around null views
                .switchMap<View> { state ->
                    val view = (state.firstVisibleItem..state.lastVisibleItem)
                            .mapNotNull { index -> llm.findViewByPosition(index) }
                            .maxBy { child -> recyclerView.weightChildVisibility(child) }

                    return@switchMap if (view != null) Observable.just(view) else Observable.empty()
                }
                .distinctUntilChanged()
                .doOnNext { currentPosition = binding.postList.getChildAdapterPosition(it) }
                .subscribe(
                        { mostVisibleChild ->
                            playerHoldersPool.pauseCurrent() // pause playback when the focus changes
                            val holder = recyclerView.findContainingViewHolder(mostVisibleChild)
                            (holder as PostViewHolder).showVideoIfNeeded()
                        },
                        { Timber.e(it, "error while observing visible items") })

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

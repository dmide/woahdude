package com.reddit.woahdude.ui

import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.reddit.woahdude.R
import com.reddit.woahdude.ui.common.BaseViewModel
import com.reddit.woahdude.model.RedditRepository
import com.reddit.woahdude.model.RedditPost
import com.reddit.woahdude.util.plusAssign
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val LAST_REFRESH_TIME = "LAST_REFRESH_TIME"
private val pagingConfig: PagedList.Config = PagedList.Config.Builder()
        .setPageSize(30)
        .setEnablePlaceholders(true)
        .build()

class ListViewModel : BaseViewModel() {
    @Inject
    lateinit var repository: RedditRepository
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    val loadingVisibility: MutableLiveData<Boolean> = MutableLiveData()
    val refreshMessage: MutableLiveData<RefreshMessage> = MutableLiveData()
    val posts: LiveData<PagedList<RedditPost>> by lazy {
        LivePagedListBuilder<Int, RedditPost>(repository.getCachedPosts(), pagingConfig)
                .setBoundaryCallback(RedditBoundaryCallback())
                .build()
    }

    private val compositeDisposable: CompositeDisposable = CompositeDisposable()

    override fun onCreated() {
        loadingVisibility.value = false

        compositeDisposable += repository.status
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { status ->
                    when (status) {
                        is RedditRepository.Status.LoadingStarted -> {
                            loadingVisibility.value = true
                            refreshMessage.value = null
                        }
                        is RedditRepository.Status.LoadingFinished -> {
                            loadingVisibility.value = false
                        }
                        is RedditRepository.Status.LoadingFailed -> {
                            Timber.e(status.t, "onRetrievePostListError")
                            refreshMessage.value = RefreshMessage(R.string.error_loading, R.string.retry)
                        }
                    }
                }
    }

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.dispose()
    }

    fun refresh() {
        compositeDisposable += repository.refreshPosts()
    }

    inner class RedditBoundaryCallback : PagedList.BoundaryCallback<RedditPost>() {
        override fun onZeroItemsLoaded() {
            super.onZeroItemsLoaded()
            if (repository.isRequestInProgress) return

            compositeDisposable += repository.requestPosts()
            sharedPreferences.edit { putLong(LAST_REFRESH_TIME, System.currentTimeMillis()) }
        }

        override fun onItemAtEndLoaded(itemAtEnd: RedditPost) {
            super.onItemAtEndLoaded(itemAtEnd)
            if (repository.isRequestInProgress) return

            compositeDisposable += repository.requestPosts(after = itemAtEnd.nextPageToken)
        }

        override fun onItemAtFrontLoaded(itemAtFront: RedditPost) {
            super.onItemAtFrontLoaded(itemAtFront)
            val now = System.currentTimeMillis()
            // sharedPreferences have an in-memory cache internally so it's ok
            val lastRefreshTime = sharedPreferences.getLong("LAST_REFRESH_TIME", now)
            if (now - lastRefreshTime > TimeUnit.HOURS.toMillis(4)) {
                refreshMessage.value = RefreshMessage(R.string.new_posts_available, R.string.refresh)
                sharedPreferences.edit { putLong(LAST_REFRESH_TIME, System.currentTimeMillis()) }
            }
        }
    }

    data class RefreshMessage(@StringRes val text: Int, @StringRes val actionText: Int)
}
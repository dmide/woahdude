package com.reddit.woahdude.ui.feed

import android.os.Handler
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.reddit.woahdude.R
import com.reddit.woahdude.common.LocalStorage
import com.reddit.woahdude.model.RedditPost
import com.reddit.woahdude.model.RedditRepository
import com.reddit.woahdude.ui.common.BaseViewModel
import com.reddit.woahdude.util.plusAssign
import com.reddit.woahdude.video.holder.VideoPlayerHoldersPool
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private val pagingConfig: PagedList.Config = PagedList.Config.Builder()
    .setPageSize(30)
    .setEnablePlaceholders(true)
    .build()

class ListViewModel : BaseViewModel() {
    @Inject
    lateinit var repository: RedditRepository
    @Inject
    lateinit var localStorage: LocalStorage
    @Inject
    lateinit var playerHoldersPool: VideoPlayerHoldersPool
    @Inject
    lateinit var handler: Handler

    private val _loadingVisibility: MutableLiveData<Boolean> = MutableLiveData()
    private val _refreshMessage: MutableLiveData<RefreshMessage> = MutableLiveData()
    private val requestStream = PublishSubject.create<RequestType>()
    private val compositeDisposable: CompositeDisposable = CompositeDisposable()

    var lastViewedPosition: Int
        get() = localStorage.lastViewedPosition
        set(value) {
            localStorage.lastViewedPosition = value
        }

    val loadingVisibility: LiveData<Boolean> get() = _loadingVisibility
    val refreshMessage: LiveData<RefreshMessage> get() = _refreshMessage
    val posts: LiveData<PagedList<RedditPost>> by lazy {
        LivePagedListBuilder<Int, RedditPost>(repository.getPosts(), pagingConfig)
            .setBoundaryCallback(RedditBoundaryCallback())
            .build()
    }

    override fun onCreated() {
        _loadingVisibility.value = false

        compositeDisposable += repository.status
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { status ->
                    when (status) {
                        is RedditRepository.Status.LoadingStarted -> {
                            _loadingVisibility.value = true
                            _refreshMessage.value = null
                        }
                        is RedditRepository.Status.LoadingFinished -> {
                            _loadingVisibility.value = false
                        }
                        is RedditRepository.Status.LoadingFailed -> {
                            Timber.e(status.t, "onRetrievePostListError")
                            _refreshMessage.value = RefreshMessage(R.string.error_loading, R.string.retry)
                        }
                    }
                }

        compositeDisposable += requestStream
                .distinctUntilChanged()
                .concatMapSingle { requestType ->
                    when (requestType) {
                        is RequestType.Request -> repository.requestPosts(requestType.after).onErrorReturn {} // errors handled in 'status' pipe
                        is RequestType.Clear -> repository.clearPosts()
                    }
                }
                .subscribe()
    }

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.dispose()
    }

    fun refreshPosts() {
        requestStream.onNext(RequestType.Clear)
        requestStream.onNext(RequestType.Request())
    }

    fun getTitle() = localStorage.settingsState.selectedSubredditTitle

    inner class RedditBoundaryCallback : PagedList.BoundaryCallback<RedditPost>() {
        override fun onZeroItemsLoaded() {
            requestStream.onNext(RequestType.Request())
            localStorage.lastRefreshTime = System.currentTimeMillis()
        }

        override fun onItemAtEndLoaded(itemAtEnd: RedditPost) {
            requestStream.onNext(RequestType.Request(after = itemAtEnd.nextPageToken))
        }

        override fun onItemAtFrontLoaded(itemAtFront: RedditPost) {
            val now = System.currentTimeMillis()
            if (localStorage.lastRefreshTime != 0L && now - localStorage.lastRefreshTime > TimeUnit.HOURS.toMillis(4)) {
                _refreshMessage.value = RefreshMessage(R.string.new_posts_available, R.string.refresh)
                localStorage.lastRefreshTime = System.currentTimeMillis()
            }
        }
    }

    data class RefreshMessage(@StringRes val text: Int, @StringRes val actionText: Int)

    sealed class RequestType {
        data class Request(val after: String? = null) : RequestType()
        object Clear : RequestType()
    }
}
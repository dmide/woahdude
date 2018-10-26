package com.reddit.woahdude.ui

import android.util.Log
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.reddit.woahdude.R
import com.reddit.woahdude.common.BaseViewModel
import com.reddit.woahdude.model.RedditDao
import com.reddit.woahdude.model.RedditRepository
import com.reddit.woahdude.network.RedditPost
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

class ListViewModel : BaseViewModel() {
    @Inject
    lateinit var redditDao: RedditDao
    @Inject
    lateinit var repository: RedditRepository

    val loadingVisibility: MutableLiveData<Int> = MutableLiveData()
    val errorMessage: MutableLiveData<Int> = MutableLiveData()
    val posts: LiveData<PagedList<RedditPost>> by lazy { initializedPagedListLiveData() }

    private val compositeDisposable: CompositeDisposable = CompositeDisposable()

    override fun onCreated() {
        loadingVisibility.value = View.GONE

        val disposable = repository.status
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { status ->
                    when (status) {
                        is RedditRepository.Status.LoadingStarted -> {
                            loadingVisibility.value = View.VISIBLE
                            errorMessage.value = null
                        }
                        is RedditRepository.Status.LoadingFinished -> {
                            loadingVisibility.value = View.GONE
                        }
                        is RedditRepository.Status.LoadingFailed -> {
                            Log.e(javaClass.name, "onRetrievePostListError", status.t)
                            errorMessage.value = R.string.error_loading
                        }
                    }
                }
        compositeDisposable.add(disposable)
    }

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.dispose()
    }

    fun refresh() {
        compositeDisposable.add(repository.refresh())
    }

    private fun initializedPagedListLiveData(): LiveData<PagedList<RedditPost>> {
        val config = PagedList.Config.Builder()
                .setPageSize(30)
                .setEnablePlaceholders(false)
                .build()

        val livePageListBuilder = LivePagedListBuilder<Int, RedditPost>(redditDao.posts(), config)
        livePageListBuilder.setBoundaryCallback(RedditBoundaryCallback())

        return livePageListBuilder.build()
    }

    inner class RedditBoundaryCallback : PagedList.BoundaryCallback<RedditPost>() {
        override fun onZeroItemsLoaded() {
            super.onZeroItemsLoaded()
            if (repository.isRequestInProgress) {
                return
            }
            repository.isRequestInProgress = true
            compositeDisposable.add(repository.requestPosts())
        }

        override fun onItemAtEndLoaded(itemAtEnd: RedditPost) {
            super.onItemAtEndLoaded(itemAtEnd)
            if (repository.isRequestInProgress) {
                return
            }
            repository.isRequestInProgress = true
            compositeDisposable.add(repository.requestPosts(after = itemAtEnd.name))
        }
    }
}
package com.reddit.woahdude.model

import com.reddit.woahdude.common.LocalStorage
import com.reddit.woahdude.model.db.RedditDb
import com.reddit.woahdude.model.network.PostsResponse
import com.reddit.woahdude.model.network.RedditApi
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class RedditRepository @Inject internal constructor(
    private val redditApi: RedditApi,
    private val redditDb: RedditDb,
    private val localStorage: LocalStorage
) {

    val status: PublishSubject<Status> = PublishSubject.create()

    fun requestPosts(after: String? = null): Single<Unit> {
        return redditApi.getPosts(subreddit = localStorage.selectedSubreddit, after = after)
                .subscribeOn(Schedulers.io())
                .doOnSubscribe { status.onNext(Status.LoadingStarted) }
                .flatMap { response -> filterAndStore(response, response.data.after) }
                .doOnEvent { _, e ->
                    status.onNext(Status.LoadingFinished)
                    e?.let { status.onNext(Status.LoadingFailed(it)) }
                }
    }

    fun clearPosts(): Single<Unit> = clearDB()

    fun getPosts() = redditDb.postDao().posts()

    private fun filterAndStore(response: PostsResponse, nextPageToken: String?): Single<Unit> {
        return Single.fromCallable {
            val isFilterEnabled = localStorage.isFilteringNonMediaPosts
            val posts = response.data.children.filter {
                val post = it.data
                !isFilterEnabled || post.getImageResource() != null || post.getVideoUrl() != null
            }

            redditDb.runInTransaction {
                val start = redditDb.postDao().getNextIndex()
                val items = posts.mapIndexed { index, child ->
                    child.data.indexInResponse = start + index
                    child.data.nextPageToken = nextPageToken
                    child.data
                }
                redditDb.postDao().insert(items)
            }
        }.subscribeOn(Schedulers.io())
    }

    private fun clearDB(): Single<Unit> {
        return Single.fromCallable {
            redditDb.postDao().deleteAll()
        }.subscribeOn(Schedulers.io())
    }

    sealed class Status {
        object LoadingStarted : Status()
        object LoadingFinished : Status()
        data class LoadingFailed(val t: Throwable) : Status()
    }
}

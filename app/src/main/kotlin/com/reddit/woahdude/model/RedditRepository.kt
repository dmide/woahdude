package com.reddit.woahdude.model

import com.reddit.woahdude.network.PostsResponse
import com.reddit.woahdude.network.RedditApi
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class RedditRepository @Inject constructor(val redditApi: RedditApi, val redditDb: RedditDb) {

    @Volatile
    var isRequestInProgress = false

    val status: PublishSubject<Status> = PublishSubject.create()

    fun requestPosts(after: String? = null): Disposable {
        return redditApi.getPosts(after = after)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { status.onNext(Status.LoadingStarted) }
                .doOnTerminate { status.onNext(Status.LoadingFinished) }
                .observeOn(Schedulers.io())
                .subscribe(
                        { response ->
                            insertPostsIntoDB(response, response.data.after)
                            isRequestInProgress = false
                        },
                        {
                            isRequestInProgress = false
                            status.onNext(Status.LoadingFailed(it))
                        }
                )
    }

    fun insertPostsIntoDB(response: PostsResponse, nextPageToken: String?) {
        response.data.children.let { posts ->
            redditDb.runInTransaction {
                val start = redditDb.postDao().getNextIndex()
                val items = posts.mapIndexed { index, child ->
                    child.data.indexInResponse = start + index
                    child.data.nextPageToken = nextPageToken
                    child.data
                }
                redditDb.postDao().insert(items)
            }
        }
    }

    fun refresh(): Disposable {
        return redditApi.getPosts()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { status.onNext(Status.LoadingStarted) }
                .doOnTerminate { status.onNext(Status.LoadingFinished) }
                .observeOn(Schedulers.io())
                .subscribe(
                        { response ->
                            redditDb.runInTransaction {
                                redditDb.postDao().deleteAll()
                            }
                            insertPostsIntoDB(response, response.data.after)
                            isRequestInProgress = false
                        },
                        {
                            isRequestInProgress = false
                            status.onNext(Status.LoadingFailed(it))
                        }
                )
    }

    sealed class Status {
        object LoadingStarted : Status()
        object LoadingFinished : Status()
        data class LoadingFailed(val t: Throwable) : Status()
    }
}

package com.reddit.woahdude.model

import com.reddit.woahdude.model.db.RedditDb
import com.reddit.woahdude.model.network.PostsResponse
import com.reddit.woahdude.model.network.RedditApi
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class RedditRepository @Inject internal constructor(private val redditApi: RedditApi, private val redditDb: RedditDb) {

    @Volatile
    var isRequestInProgress = false
        private set
    val status: PublishSubject<Status> = PublishSubject.create()

    fun requestPosts(after: String? = null) = requestPosts(after, null)

    fun refreshPosts() = requestPosts(null) {
        redditDb.runInTransaction {
            redditDb.postDao().deleteAll()
        }
    }

    fun getCachedPosts() = redditDb.postDao().posts()

    private fun requestPosts(after: String? = null, onSuccess : ((response: PostsResponse) -> Unit)? = null): Disposable {
        isRequestInProgress = true

        return redditApi.getPosts(after = after)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { status.onNext(Status.LoadingStarted) }
                .doOnTerminate { status.onNext(Status.LoadingFinished) }
                .observeOn(Schedulers.io())
                .subscribe(
                        { response ->
                            onSuccess?.invoke(response)
                            insertPostsIntoDB(response, response.data.after)
                            isRequestInProgress = false
                        },
                        {
                            isRequestInProgress = false
                            status.onNext(Status.LoadingFailed(it))
                        }
                )
    }

    private fun insertPostsIntoDB(response: PostsResponse, nextPageToken: String?) {
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

    sealed class Status {
        object LoadingStarted : Status()
        object LoadingFinished : Status()
        data class LoadingFailed(val t: Throwable) : Status()
    }
}

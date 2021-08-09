package com.reddit.woahdude.common

import android.content.Context
import com.reddit.woahdude.util.bindSharedPreference

private const val LAST_REFRESH_TIME = "LAST_REFRESH_TIME"
private const val FILTER_NON_MEDIA_POSTS = "FILTER_NON_MEDIA_POSTS"

// to abstract away from android-package SharedPrefs to support testing
class LocalStorage(val context: Context) {

    var lastRefreshTime by bindSharedPreference(context, LAST_REFRESH_TIME, 0L)
    var isFilteringNonMediaPosts by bindSharedPreference(context, FILTER_NON_MEDIA_POSTS, true)

}
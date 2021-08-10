package com.reddit.woahdude.common

import android.content.Context
import com.reddit.woahdude.ui.settings.SettingsState
import com.reddit.woahdude.util.bindSharedPreference

private const val LAST_REFRESH_TIME = "LAST_REFRESH_TIME"
private const val LAST_VIEWED_POSITION = "LAST_VIEWED_POSITION"
private const val FILTER_NON_MEDIA_POSTS = "FILTER_NON_MEDIA_POSTS"
private const val SUBREDDIT = "SUBREDDIT"
private const val SUBREDDIT_DEFAULT = "woahdude"

// to abstract away from android-package SharedPrefs to support testing
class LocalStorage(val context: Context) {

    var lastRefreshTime by bindSharedPreference(context, LAST_REFRESH_TIME, 0L)
    var lastViewedPosition by bindSharedPreference(context, LAST_VIEWED_POSITION, 0)
    var isFilteringNonMediaPosts by bindSharedPreference(context, FILTER_NON_MEDIA_POSTS, true)
    var selectedSubreddit by bindSharedPreference(context, SUBREDDIT, SUBREDDIT_DEFAULT)

    val settingsState get() = SettingsState(
        isFilteringNonMediaPosts,
        selectedSubreddit
    )
}
package com.reddit.woahdude.common

import android.content.Context
import com.reddit.woahdude.util.bindSharedPreference

private const val LAST_REFRESH_TIME = "LAST_REFRESH_TIME"

class LocalStorage(val context: Context) {

    var lastRefreshTime by bindSharedPreference(context, LAST_REFRESH_TIME, 0L)

}
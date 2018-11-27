package com.reddit.woahdude.common

import android.util.Log
import com.crashlytics.android.Crashlytics

import timber.log.Timber

class ReleaseTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        Crashlytics.log(priority, tag, message)
        if (priority == Log.ERROR) {
            Crashlytics.logException(t)
        }
    }
}
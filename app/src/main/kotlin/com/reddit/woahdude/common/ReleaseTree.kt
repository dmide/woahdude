package com.reddit.woahdude.common

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

import timber.log.Timber

class ReleaseTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        FirebaseCrashlytics.getInstance().log(message)
        if (priority == Log.ERROR && t != null) {
            FirebaseCrashlytics.getInstance().recordException(t)
        }
    }
}
package com.reddit.woahdude.common

import android.app.Application
import com.crashlytics.android.Crashlytics
import com.reddit.woahdude.BuildConfig
import com.reddit.woahdude.model.db.DBModule
import com.reddit.woahdude.model.network.NetworkModule
import io.fabric.sdk.android.Fabric
import timber.log.Timber
import timber.log.Timber.DebugTree

class WDApplication : Application() {

    val component: AppComponent by lazy {
        DaggerAppComponent
                .builder()
                .appModule(AppModule(this))
                .dbModule(DBModule)
                .networkModule(NetworkModule)
                .build()
    }

    override fun onCreate() {
        super.onCreate()
        Fabric.with(this, Crashlytics())
        Timber.plant(if (BuildConfig.DEBUG) DebugTree() else ReleaseTree())
    }
}
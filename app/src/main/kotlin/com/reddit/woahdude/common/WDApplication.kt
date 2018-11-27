package com.reddit.woahdude.common

import android.app.Application
import com.reddit.woahdude.BuildConfig
import com.reddit.woahdude.inject.component.AppComponent
import com.reddit.woahdude.inject.component.DaggerAppComponent
import com.reddit.woahdude.inject.module.AppModule
import com.reddit.woahdude.inject.module.ModelModule
import com.reddit.woahdude.inject.module.NetworkModule
import timber.log.Timber
import timber.log.Timber.DebugTree
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric

class WDApplication : Application() {

    val component: AppComponent by lazy {
        DaggerAppComponent
                .builder()
                .appModule(AppModule(this))
                .dbModule(ModelModule)
                .networkModule(NetworkModule)
                .build()
    }

    override fun onCreate() {
        super.onCreate()
        Fabric.with(this, Crashlytics())
        Timber.plant(if (BuildConfig.DEBUG) DebugTree() else ReleaseTree())
    }
}
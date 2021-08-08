package com.reddit.woahdude.common

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex
import com.reddit.woahdude.BuildConfig
import com.reddit.woahdude.model.db.DBModule
import com.reddit.woahdude.model.network.NetworkModule
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
        Timber.plant(if (BuildConfig.DEBUG) DebugTree() else ReleaseTree())
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }
}
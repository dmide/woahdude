package com.reddit.woahdude.common

import android.app.Application
import com.reddit.woahdude.inject.component.AppComponent
import com.reddit.woahdude.inject.component.DaggerAppComponent
import com.reddit.woahdude.inject.module.AppModule
import com.reddit.woahdude.inject.module.ModelModule
import com.reddit.woahdude.inject.module.NetworkModule

class WDApplication : Application() {

    val component: AppComponent by lazy {
        DaggerAppComponent
                .builder()
                .appModule(AppModule(this))
                .dbModule(ModelModule)
                .networkModule(NetworkModule)
                .build()
    }

}
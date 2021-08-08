package com.reddit.woahdude.app

import android.content.Context
import android.content.res.Resources
import android.os.Handler
import android.os.Looper
import com.reddit.woahdude.common.LocalStorage
import dagger.Module
import dagger.Provides
import javax.inject.Singleton


@Module
@Suppress("unused")
class AppModule(private val appContext: Context) {

    @Provides
    fun provideAppContext(): Context {
        return appContext
    }

    @Provides
    fun provideResources(): Resources {
        return appContext.resources
    }

    @Provides
    @Singleton
    fun provideMainHandler(): Handler {
        return Handler(Looper.getMainLooper())
    }

    @Provides
    @Singleton
    fun provideLocalStorage(context: Context): LocalStorage {
        return LocalStorage(context)
    }
}
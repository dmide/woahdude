package com.reddit.woahdude.common

import android.content.Context
import android.content.res.Resources
import dagger.Module
import dagger.Provides
import javax.inject.Singleton


@Module
@Suppress("unused")
class AppModule(val appContext: Context) {

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
    fun provideLocalStorage(context: Context): LocalStorage {
        return LocalStorage(context)
    }
}
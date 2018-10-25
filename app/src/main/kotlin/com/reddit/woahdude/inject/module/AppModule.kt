package com.reddit.woahdude.inject.module

import android.content.Context
import android.content.res.Resources
import dagger.Module
import dagger.Provides

@Module
@Suppress("unused")
class AppModule(val appContext: Context) {

    @Provides
    fun provideAppContext(): Context {
        return appContext
    }

    @Provides
    fun provideResources(): Resources{
        return appContext.resources
    }

}
package com.reddit.woahdude.common

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.preference.PreferenceManager
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
    fun provideResources(): Resources {
        return appContext.resources
    }

    @Provides
    fun provideSharedPreferences(): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(appContext)
    }
}
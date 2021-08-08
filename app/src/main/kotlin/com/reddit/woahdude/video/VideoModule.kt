package com.reddit.woahdude.video

import android.content.Context
import com.google.android.exoplayer2.upstream.DataSource
import com.reddit.woahdude.util.megabytes
import dagger.Module
import dagger.Provides
import dagger.Reusable

@Module
@Suppress("unused")
object VideoModule {

    @Provides
    @Reusable
    @JvmStatic
    internal fun provideDataSourceFactory(context: Context): DataSource.Factory {
        return CacheDataSourceFactory(context, 100.megabytes, 20.megabytes)
    }
}
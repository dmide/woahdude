package com.reddit.woahdude.video

import android.content.Context
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.upstream.cache.CacheDataSink
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.android.exoplayer2.util.Util
import java.io.File

internal class CacheDataSourceFactory(private val context: Context,
                                      private val maxCacheSize: Long,
                                      private val maxFileSize: Long) : DataSource.Factory {

    private val defaultDatasourceFactory: DefaultDataSourceFactory
    private val simpleCache: SimpleCache by lazy {
        val evictor = LeastRecentlyUsedCacheEvictor(maxCacheSize)
        SimpleCache(File(context.cacheDir, "media"), evictor)
    }

    init {
        val userAgent = Util.getUserAgent(context, context.packageName)
        val bandwidthMeter = DefaultBandwidthMeter()
        defaultDatasourceFactory = DefaultDataSourceFactory(context,
                bandwidthMeter,
                DefaultHttpDataSourceFactory(userAgent, bandwidthMeter))
    }

    override fun createDataSource(): DataSource {
        return CacheDataSource(simpleCache,
                defaultDatasourceFactory.createDataSource(),
                FileDataSource(),
                CacheDataSink(simpleCache, maxFileSize),
                CacheDataSource.FLAG_BLOCK_ON_CACHE or CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR,
                null)
    }
}
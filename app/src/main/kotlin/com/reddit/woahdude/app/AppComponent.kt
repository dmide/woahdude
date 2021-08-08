package com.reddit.woahdude.app

import com.reddit.woahdude.model.db.DBModule
import com.reddit.woahdude.model.network.NetworkModule
import com.reddit.woahdude.model.RedditRepository
import com.reddit.woahdude.model.Video
import com.reddit.woahdude.ui.ListActivity
import com.reddit.woahdude.ui.ListViewModel
import com.reddit.woahdude.ui.PostViewHolder
import com.reddit.woahdude.video.VideoModule
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class, NetworkModule::class, DBModule::class, VideoModule::class])
interface AppComponent {

    fun inject(listActivity: ListActivity)
    
    fun inject(listViewModel: ListViewModel)

    fun inject(redditRepository: RedditRepository)

    fun inject(postViewHolder: PostViewHolder)

    @Component.Builder
    interface Builder {
        fun build(): AppComponent

        fun appModule(appModule: AppModule): Builder

        fun dbModule(dbModule: DBModule): Builder

        fun networkModule(networkModule: NetworkModule): Builder

        fun videoModule(videoModule: VideoModule): Builder
    }
}
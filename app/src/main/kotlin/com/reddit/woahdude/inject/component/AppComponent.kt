package com.reddit.woahdude.inject.component

import com.reddit.woahdude.inject.module.AppModule
import com.reddit.woahdude.inject.module.ModelModule
import com.reddit.woahdude.inject.module.NetworkModule
import com.reddit.woahdude.model.RedditRepository
import com.reddit.woahdude.ui.ListActivity
import com.reddit.woahdude.ui.ListViewModel
import com.reddit.woahdude.ui.PostViewHolder
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class, NetworkModule::class, ModelModule::class])
interface AppComponent {

    fun inject(listActivity: ListActivity)
    
    fun inject(listViewModel: ListViewModel)

    fun inject(redditRepository: RedditRepository)

    fun inject(postViewHolder: PostViewHolder)

    @Component.Builder
    interface Builder {
        fun build(): AppComponent

        fun appModule(appModule: AppModule): Builder

        fun dbModule(dbModule: ModelModule): Builder

        fun networkModule(networkModule: NetworkModule): Builder
    }
}
package com.reddit.woahdude.common

import androidx.lifecycle.ViewModel
import com.reddit.woahdude.inject.component.AppComponent
import com.reddit.woahdude.ui.ListViewModel

abstract class BaseViewModel() : ViewModel() {

    fun inject(component: AppComponent) {
        when (this) {
            is ListViewModel -> component.inject(this)
        }
        onCreated()
    }

    abstract fun onCreated()
}
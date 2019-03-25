package com.reddit.woahdude.ui.common

import androidx.lifecycle.ViewModel
import com.reddit.woahdude.common.AppComponent
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
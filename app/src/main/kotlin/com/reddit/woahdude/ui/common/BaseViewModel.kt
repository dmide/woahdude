package com.reddit.woahdude.ui.common

import androidx.lifecycle.ViewModel
import com.reddit.woahdude.app.AppComponent
import com.reddit.woahdude.ui.list.ListViewModel
import com.reddit.woahdude.ui.settings.SettingsViewModel

abstract class BaseViewModel() : ViewModel() {

    fun inject(component: AppComponent) {
        when (this) {
            is ListViewModel -> component.inject(this)
            is SettingsViewModel -> component.inject(this)
        }
        onCreated()
    }

    abstract fun onCreated()
}
package com.reddit.woahdude.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.reddit.woahdude.app.AppComponent

class ViewModelFactory(private val component: AppComponent) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (BaseViewModel::class.java.isAssignableFrom(modelClass)) {
            val instance = modelClass.newInstance() as T
            (instance as BaseViewModel).inject(component)
            return instance
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
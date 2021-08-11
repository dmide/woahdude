package com.reddit.woahdude.ui.common

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.reddit.woahdude.util.Metrics

open class BaseActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Metrics.init(this)
    }
}
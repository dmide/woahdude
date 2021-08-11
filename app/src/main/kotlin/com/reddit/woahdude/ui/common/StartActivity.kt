package com.reddit.woahdude.ui.common

import android.content.Intent
import android.os.Bundle
import com.reddit.woahdude.app.WDApplication
import com.reddit.woahdude.common.LocalStorage
import com.reddit.woahdude.ui.feed.ListActivity
import com.reddit.woahdude.ui.feed.PagerActivity
import javax.inject.Inject

class StartActivity : BaseActivity() {

    @Inject
    lateinit var localStorage: LocalStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val component = (application as WDApplication).component
        component.inject(this)

        if (localStorage.isPagerLayoutEnabled) {
            startActivity(Intent(this, PagerActivity::class.java))
        } else {
            startActivity(Intent(this, ListActivity::class.java))
        }
        overridePendingTransition(0, 0)
        finish()
    }
}
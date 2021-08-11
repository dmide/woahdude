package com.reddit.woahdude.ui.feed

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import com.reddit.woahdude.R
import com.reddit.woahdude.app.WDApplication
import com.reddit.woahdude.ui.common.BaseActivity
import com.reddit.woahdude.ui.common.StartActivity
import com.reddit.woahdude.ui.common.ViewModelFactory
import com.reddit.woahdude.ui.settings.SettingsActivity

abstract class FeedActivity: BaseActivity() {
    protected lateinit var viewModel: ListViewModel

    protected var currentPosition: Int = 0
    protected var isVideoPoolCleared = false

    private var snackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val component = (application as WDApplication).component
        viewModel = ViewModelProviders.of(this, ViewModelFactory(component)).get(ListViewModel::class.java)

        currentPosition = viewModel.lastViewedPosition

        viewModel.refreshMessage.observe(this, Observer { message ->
            if (message != null) showRefreshSnack(message) else hideRefreshSnack()
        })
    }

    override fun onPause() {
        viewModel.lastViewedPosition = currentPosition
        viewModel.playerHoldersPool.pauseCurrent()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        viewModel.playerHoldersPool.resumeCurrent()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SettingsActivity.SETTINGS_REQUEST_CODE) {
            when(resultCode) {
                SettingsActivity.SETTINGS_RESULT_RESTART_NEEDED -> {
                    finish()
                    viewModel.playerHoldersPool.clear()
                    isVideoPoolCleared = true
                    startActivity(Intent(this, StartActivity::class.java))
                }
                SettingsActivity.SETTINGS_RESULT_REFRESH_NEEDED -> {
                    viewModel.refreshPosts()
                    setTitle(viewModel.getTitle())
                }
            }
        }
    }

    override fun onDestroy() {
        if (!isVideoPoolCleared) {
            viewModel.playerHoldersPool.clear()
        }
        super.onDestroy()
    }

    abstract fun setTitle(title: String)

    abstract fun getRootView(): View

    protected fun setupSwipeRefreshLayout(srl: SwipeRefreshLayout) {
        srl.apply {
            setOnRefreshListener { viewModel.refreshPosts() }
            val showProgress = Runnable { isRefreshing = true }
            viewModel.loadingVisibility.observe(this@FeedActivity, Observer { isLoading ->
                if (!isLoading) {
                    removeCallbacks(showProgress)
                    isRefreshing = false
                } else {
                    postDelayed(showProgress, 1000) //show progress only after 1 second of loading
                }
            })
        }
    }

    protected fun setupToolbar(toolbar: Toolbar) {
        toolbar.apply {
            title = viewModel.getTitle()
            inflateMenu(R.menu.menu_main)
            setOnMenuItemClickListener { item ->
                when(item.itemId) {
                    R.id.settings -> {
                        startActivityForResult(
                            Intent(this@FeedActivity, SettingsActivity::class.java),
                            SettingsActivity.SETTINGS_REQUEST_CODE
                        )
                    }
                    R.id.feed_mode -> {
                        viewModel.localStorage.isPagerLayoutEnabled = !viewModel.localStorage.isPagerLayoutEnabled
                        finish()
                        viewModel.playerHoldersPool.clear()
                        isVideoPoolCleared = true
                        startActivity(Intent(this@FeedActivity, StartActivity::class.java))
                    }
                }
                return@setOnMenuItemClickListener true
            }
        }
    }

    private fun showRefreshSnack(message: ListViewModel.RefreshMessage) {
        hideRefreshSnack()
        snackbar = Snackbar.make(getRootView(), message.text, Snackbar.LENGTH_INDEFINITE)
            .setAction(message.actionText) { viewModel.refreshPosts() }
            .apply { show() }
    }

    private fun hideRefreshSnack() {
        snackbar?.dismiss()
    }
}
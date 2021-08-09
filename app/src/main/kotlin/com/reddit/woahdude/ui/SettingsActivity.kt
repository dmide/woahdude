package com.reddit.woahdude.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.reddit.woahdude.R
import com.reddit.woahdude.app.WDApplication
import com.reddit.woahdude.common.LocalStorage
import com.reddit.woahdude.databinding.ActivitySettingsBinding
import com.reddit.woahdude.ui.common.BaseActivity
import javax.inject.Inject


class SettingsActivity : BaseActivity() {
    companion object {
        const val SETTINGS_REQUEST_CODE = 1337
        const val SETTINGS_RESULT_REFRESH_NEEDED = SETTINGS_REQUEST_CODE + 1
    }

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var initialState: State
    private var newState: State? = null

    @Inject
    lateinit var localStorage: LocalStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val component = (application as WDApplication).component
        component.inject(this)

        initialState = State(localStorage.isFilteringNonMediaPosts)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_settings)
        binding.toolbar.title = getString(R.string.settings)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        binding.privacyPolicy.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.privacy_policy_url)));
            startActivity(browserIntent);
        }

        binding.filterSwitch.isChecked = localStorage.isFilteringNonMediaPosts
        binding.filterContainer.setOnClickListener {
            binding.filterSwitch.isChecked = !binding.filterSwitch.isChecked
            val isFiltering = binding.filterSwitch.isChecked
            localStorage.isFilteringNonMediaPosts = isFiltering
            newState = (newState ?: initialState).copy(isFilteringNonMediaPosts = isFiltering)
        }
    }

    override fun onBackPressed() {
        if (initialState != newState) {
            setResult(SETTINGS_RESULT_REFRESH_NEEDED)
        }
        super.onBackPressed()
    }

    private data class State(val isFilteringNonMediaPosts: Boolean)
}
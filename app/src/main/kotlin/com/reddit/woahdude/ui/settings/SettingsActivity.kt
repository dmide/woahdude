package com.reddit.woahdude.ui.settings

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import com.reddit.woahdude.R
import com.reddit.woahdude.app.WDApplication
import com.reddit.woahdude.databinding.ActivitySettingsBinding
import com.reddit.woahdude.ui.common.BaseActivity
import com.reddit.woahdude.ui.common.ViewModelFactory
import com.reddit.woahdude.util.addTo
import io.reactivex.disposables.CompositeDisposable


class SettingsActivity : BaseActivity() {
    companion object {
        const val SETTINGS_REQUEST_CODE = 1337
        const val SETTINGS_RESULT_REFRESH_NEEDED = SETTINGS_REQUEST_CODE + 1
    }

    private val compositeDisposable = CompositeDisposable()

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var viewModel: SettingsViewModel

    private var dialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val component = (application as WDApplication).component
        viewModel = ViewModelProviders.of(this, ViewModelFactory(component)).get(SettingsViewModel::class.java)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_settings)

        viewModel.stateObservable.subscribe { state ->
            binding.filterSwitch.isChecked = state.isFilteringNonMediaPosts
            binding.subredditValue.text = state.selectedSubredditTitle
        }.addTo(compositeDisposable)

        binding.toolbar.title = getString(R.string.settings)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        binding.privacyPolicy.setOnClickListener {
            viewModel.onPrivacyPolicyClick(this)
        }

        binding.filterContainer.setOnClickListener {
            viewModel.onFilterToggled()
        }

        binding.subredditContainer.setOnClickListener {
            showSubredditSelectionDialog()
        }
    }

    override fun onBackPressed() {
        if (viewModel.isStateChanged()) {
            setResult(SETTINGS_RESULT_REFRESH_NEEDED)
        }
        super.onBackPressed()
    }

    private fun showSubredditSelectionDialog() {
        val subreddits = resources.getStringArray(R.array.subreddits)
        val selectedSubredditTitle = viewModel.currentState.selectedSubredditTitle
        val selectedIndex = subreddits.indexOf(selectedSubredditTitle).let {
            if (it == -1) subreddits.size - 1
            else it
        }

        dialog = AlertDialog.Builder(this)
            .setTitle(R.string.select_subreddit)
            .setSingleChoiceItems(R.array.subreddits, selectedIndex) { _, which ->
                if (which == subreddits.size - 1) {
                    showTextInputDialog()
                } else {
                    viewModel.onNewSubredditSelected(subreddits[which])
                }
                dialog?.dismiss()
            }.show()
    }

    private fun showTextInputDialog() {
        val inputLayout = LayoutInflater.from(this).inflate(R.layout.dialog_text_input, null, false)
        val input = inputLayout.findViewById<EditText>(R.id.editText)

        AlertDialog.Builder(this)
            .setTitle(R.string.type_subreddit)
            .setView(inputLayout)
            .setPositiveButton(R.string.ok) { _, _ ->
                viewModel.onNewSubredditSelected(input.text.toString())
            }
            .setNegativeButton(R.string.cancel) { _, _ -> return@setNegativeButton }
            .show()
    }
}
package com.reddit.woahdude.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.reddit.woahdude.R
import com.reddit.woahdude.common.LocalStorage
import com.reddit.woahdude.ui.common.BaseViewModel
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import javax.inject.Inject

class SettingsViewModel: BaseViewModel() {
    private lateinit var initialState: SettingsState

    private val stateSubject = BehaviorSubject.create<SettingsState>()

    val stateObservable: Observable<SettingsState> get() = stateSubject
    val currentState get() = stateSubject.value!!

    @Inject
    lateinit var localStorage: LocalStorage

    override fun onCreated() {
        initialState = localStorage.settingsState
        stateSubject.onNext(initialState)
    }

    fun onPrivacyPolicyClick(context: Context) {
        val browserIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.privacy_policy_url)))
        context.startActivity(browserIntent);
    }

    fun onFilterToggled() {
        val currentState = stateSubject.value!!
        val isFiltering = !currentState.isFilteringNonMediaPosts
        localStorage.isFilteringNonMediaPosts = isFiltering
        stateSubject.onNext(currentState.copy(isFilteringNonMediaPosts = isFiltering))
    }

    fun onFeedToggled() {
        val currentState = stateSubject.value!!
        val isPagerFeed = !currentState.isPagerFeed
        localStorage.isPagerLayoutEnabled = isPagerFeed
        stateSubject.onNext(currentState.copy(isPagerFeed = isPagerFeed))
    }

    fun onNewSubredditSelected(text: String) {
        val selection = text.let {
            if (it.contains("/")) {
                it.split("/")[1]
            } else {
                it
            }
        }
        val currentState = stateSubject.value!!
        localStorage.selectedSubreddit = selection
        stateSubject.onNext(currentState.copy(selectedSubreddit = selection))
    }

    fun isStateChanged() = currentState != initialState

    fun isRestartNeeded() = currentState.isPagerFeed != initialState.isPagerFeed
}
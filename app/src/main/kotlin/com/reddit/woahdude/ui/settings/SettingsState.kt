package com.reddit.woahdude.ui.settings

data class SettingsState(
    val isFilteringNonMediaPosts: Boolean,
    val selectedSubreddit: String,
    val isPagerFeed: Boolean
) {
    val selectedSubredditTitle: String get() = "r/${selectedSubreddit}"
}
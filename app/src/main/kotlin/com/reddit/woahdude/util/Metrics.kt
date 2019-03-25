package com.reddit.woahdude.util

import android.app.Activity
import android.content.Context
import android.graphics.Point
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.WindowManager

object Metrics {
    var deviceWidth: Int = 0
        private set
    var deviceHeight: Int = 0
        private set
    var statusBarHeight: Int = 0
        private set
    var navBarHeight: Int = 0
        private set
    var actionBarHeight: Int = 0
        private set
    var contentHeight: Int = 0
        private set
    var optimalContentHeight: Int = 0
        private set
    var density: Float = 0.toFloat()
        private set
    var isInitialised: Boolean = false
        private set

    fun calcDeviceMetrics(activity: Activity) {
        val metrics = DisplayMetrics()
        val windowManager = activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getMetrics(metrics)
        density = metrics.density

        val display = activity.windowManager.defaultDisplay
        val realSize = Point()
        display.getRealSize(realSize)
        val size = Point()
        display.getSize(size)

        deviceWidth = realSize.x
        deviceHeight = realSize.y
        navBarHeight = realSize.y - size.y

        val res = activity.resources
        val resourceId = res.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            statusBarHeight = res.getDimensionPixelSize(resourceId)
        }

        // Calculate ActionBar height
        val tv = TypedValue()
        if (activity.theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, res.displayMetrics)
        }

        contentHeight = deviceHeight - navBarHeight - statusBarHeight
        optimalContentHeight = contentHeight - navBarHeight

        isInitialised = true
    }
}
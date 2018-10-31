package com.reddit.woahdude.common

import android.content.res.Resources
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

val Int.dp: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()
val Int.px: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

fun RecyclerView.getChildVisiblePercent(child: View?): Int {
    if (child == null) {
        return 0
    }
    val rowRect = Rect()
    child.getGlobalVisibleRect(rowRect)

    var percent: Int
    if (rowRect.bottom >= bottom) {
        val visibleHeightFirst = bottom - rowRect.top
        percent = visibleHeightFirst * 100 / child.getHeight()
    } else {
        val visibleHeightFirst = rowRect.bottom - top
        percent = visibleHeightFirst * 100 / child.getHeight()
    }

    if (percent > 100) percent = 100
    return percent
}

fun GlideRequest<Drawable>.onFinish(callback: () -> Unit) : GlideRequest<Drawable> {
    return listener(object : RequestListener<Drawable> {
        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
            callback.invoke()
            return false
        }

        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: com.bumptech.glide.load.DataSource?, isFirstResource: Boolean): Boolean {
            callback.invoke()
            return false
        }
    })
}
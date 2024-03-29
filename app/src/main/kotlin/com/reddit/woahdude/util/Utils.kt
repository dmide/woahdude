package com.reddit.woahdude.util

import android.content.Context
import android.content.res.Resources
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Handler
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.reddit.woahdude.common.GlideRequest
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import kotlin.math.min


val Int.dp: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()
val Int.px: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()
val Int.megabytes: Long
    get() = this * 1024L * 1024L

/**
 * weights fully visible children based on proximity to parent's center
 */
fun RecyclerView.weightChildVisibility(child: View?): Int {
    var percent = getChildVisiblePercent(child)
    if (child != null && percent == 100) {
        val childCenter = child.top + (child.bottom - child.top) / 2
        val center = (bottom - top) / 2
        val distance = Math.abs(center - childCenter)
        val distancePercent = (distance / measuredHeight.toFloat()) * 100
        percent += (100 - distancePercent.toInt())
    }
    return percent
}

fun RecyclerView.getChildVisiblePercent(child: View?): Int {
    if (child == null) {
        return 0
    }
    val rowRect = Rect()
    child.getGlobalVisibleRect(rowRect)

    return if (rowRect.bottom >= bottom) {
        val visibleHeightFirst = bottom - rowRect.top
        visibleHeightFirst * 100 / child.height
    } else {
        val visibleHeightFirst = rowRect.bottom - top
        visibleHeightFirst * 100 / child.height
    }.let {
        min(it, 100)
    }
}

fun GlideRequest<Drawable>.onFinish(onSuccess: () -> Unit, onError: (Exception?) -> Unit) : GlideRequest<Drawable> {
    return listener(object : RequestListener<Drawable> {
        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
            onError.invoke(e)
            return false
        }

        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: com.bumptech.glide.load.DataSource?, isFirstResource: Boolean): Boolean {
            onSuccess.invoke()
            return false
        }
    })
}

operator fun CompositeDisposable.plusAssign(disposable: Disposable) {
    add(disposable)
}

fun Context.toast(text: CharSequence, long: Boolean = false): Toast =
        Toast.makeText(this.applicationContext, text, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).apply { show() }

fun Disposable.addTo(compositeDisposable: CompositeDisposable) {
    compositeDisposable.add(this)
}

fun Handler.clearAndPostDelayed(delay: Long, vararg actions: Runnable) {
    actions.forEach {
        removeCallbacks(it)
        postDelayed(it, delay)
    }
}
package com.reddit.woahdude.ui.components

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.drawable.Drawable

class WrappedDrawable(protected val drawable: Drawable) : Drawable() {

    override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        super.setBounds(left, top, right, bottom)
        drawable.setBounds(left, top, right, bottom)
    }

    override fun setAlpha(alpha: Int) {
        drawable.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        drawable.colorFilter = colorFilter
    }

    override fun draw(canvas: Canvas) {
        drawable.draw(canvas)
    }

    override fun getOpacity() = drawable.opacity

    override fun getIntrinsicWidth() = drawable.bounds.width()

    override fun getIntrinsicHeight() = drawable.bounds.height()
}

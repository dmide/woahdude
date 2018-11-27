package com.reddit.woahdude.ui.components

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.drawable.Drawable

class WrappedDrawable(val underlying: Drawable) : Drawable() {

    override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        super.setBounds(left, top, right, bottom)
        underlying.setBounds(left, top, right, bottom)
    }

    override fun setAlpha(alpha: Int) {
        underlying.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        underlying.colorFilter = colorFilter
    }

    override fun draw(canvas: Canvas) {
        underlying.draw(canvas)
    }

    override fun getOpacity() = underlying.opacity

    override fun getIntrinsicWidth() = underlying.bounds.width()

    override fun getIntrinsicHeight() = underlying.bounds.height()
}

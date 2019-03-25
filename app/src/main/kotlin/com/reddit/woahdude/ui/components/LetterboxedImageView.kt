package com.reddit.woahdude.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.ImageView
import com.reddit.woahdude.ui.components.WrappedDrawable

class LetterboxedImageView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0) : ImageView(context, attrs, defStyleAttr) {

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        background = null
        if (drawable != null) {
            if (isTransparent(drawable)) return
            if (drawable is WrappedDrawable && isTransparent(drawable.underlying)) return
            setBackgroundColor(Color.BLACK)
        }
    }

    override fun setImageBitmap(bm: Bitmap?) {
        super.setImageBitmap(bm)
        background = null
        if (bm != null) {
            setBackgroundColor(Color.BLACK)
        }
    }

    private fun isTransparent(drawable: Drawable?): Boolean {
        if (drawable is ColorDrawable && drawable.color == Color.TRANSPARENT) {
            return true
        }
        return false
    }
}
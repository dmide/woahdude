package com.reddit.woahdude.util

import android.content.res.Resources
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.graphics.drawable.Drawable
import android.opengl.GLES20
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.reddit.woahdude.common.GlideRequest
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext


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

/**
 * Clear the given surface Texture by attaching a GL context and clearing the surface.
 * @param texture a valid SurfaceTexture
 */
fun clearSurface(texture: SurfaceTexture?) {
    if (texture == null) {
        return
    }

    val egl = EGLContext.getEGL() as EGL10
    val display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
    egl.eglInitialize(display, null)

    val attribList = intArrayOf(EGL10.EGL_RED_SIZE, 8, EGL10.EGL_GREEN_SIZE, 8, EGL10.EGL_BLUE_SIZE, 8, EGL10.EGL_ALPHA_SIZE, 8, EGL10.EGL_RENDERABLE_TYPE, EGL10.EGL_WINDOW_BIT, EGL10.EGL_NONE, 0, // placeholder for recordable [@-3]
            EGL10.EGL_NONE)
    val configs = arrayOfNulls<EGLConfig>(1)
    val numConfigs = IntArray(1)
    egl.eglChooseConfig(display, attribList, configs, configs.size, numConfigs)
    val config = configs[0]
    val context = egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, intArrayOf(12440, 2, EGL10.EGL_NONE))
    val eglSurface = egl.eglCreateWindowSurface(display, config, texture,
            intArrayOf(EGL10.EGL_NONE))

    egl.eglMakeCurrent(display, eglSurface, eglSurface, context)
    GLES20.glClearColor(0f, 0f, 0f, 1f)
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
    egl.eglSwapBuffers(display, eglSurface)
    egl.eglDestroySurface(display, eglSurface)
    egl.eglMakeCurrent(display, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE,
            EGL10.EGL_NO_CONTEXT)
    egl.eglDestroyContext(display, context)
    egl.eglTerminate(display)
}
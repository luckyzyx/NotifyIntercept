package com.luckyzyx.notifyintercept.utlis

import android.util.Log
import com.luckyzyx.notifyintercept.BuildConfig

@Suppress("MemberVisibilityCanBePrivate")
object LogUtils {
    const val globalTag = "NotifyIntercept"
    var enable = BuildConfig.DEBUG

    fun d(tag: String, method: String, msg: String, send: Boolean = enable) {
        if (send) Log.d(globalTag, "$tag: $method -> $msg")
    }

    fun e(tag: String, method: String, msg: String, send: Boolean = enable) {
        if (send) Log.e(globalTag, "$tag: $method -> $msg")
    }

    fun i(tag: String, method: String, msg: String, send: Boolean = enable) {
        if (send) Log.i(globalTag, "$tag: $method -> $msg")
    }

    fun v(tag: String, method: String, msg: String, send: Boolean = enable) {
        if (send) Log.v(globalTag, "$tag: $method -> $msg")
    }

    fun w(tag: String, method: String, msg: String, send: Boolean = enable) {
        if (send) Log.w(globalTag, "$tag: $method -> $msg")
    }
}
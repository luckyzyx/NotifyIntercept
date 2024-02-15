package com.luckyzyx.notifyintercept.utlis

import android.graphics.drawable.Drawable
import java.io.Serializable

data class AppInfo(
    var appIcon: Drawable,
    var appName: CharSequence,
    var packName: String
) : Serializable

data class NotifyInfo(
    val title: String,
    val content: String
) : Serializable
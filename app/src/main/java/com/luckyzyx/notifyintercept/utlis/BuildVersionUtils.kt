@file:Suppress("unused")

package com.luckyzyx.notifyintercept.utlis

import android.os.Build
import com.luckyzyx.notifyintercept.BuildConfig

/**SDK_INT版本*/
val SDK get() = Build.VERSION.SDK_INT

/**Android11 30 R*/
val A11 get() = Build.VERSION_CODES.R

/**Android12 31 S*/
val A12 get() = Build.VERSION_CODES.S

/**Android13 33 TIRAMISU*/
val A13 get() = Build.VERSION_CODES.TIRAMISU

/**Android14 34 XX*/
val A14 get() = Build.VERSION_CODES.UPSIDE_DOWN_CAKE

/**
 * 获取构建版本名/版本号
 * @return [String]
 */
val getVersionName get() = BuildConfig.VERSION_NAME
val getVersionCode get() = BuildConfig.VERSION_CODE


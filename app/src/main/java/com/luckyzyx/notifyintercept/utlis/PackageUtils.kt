@file:Suppress("unused", "NewApi")

package com.luckyzyx.notifyintercept.utlis

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.ResolveInfoFlags
import android.content.pm.ResolveInfo

class PackageUtils(private val packageManager: PackageManager) {
    fun getPackageInfo(packName: String, flag: Int): PackageInfo? {
        return try {
            if (SDK < A13) packageManager.getPackageInfo(packName, flag)
            else packageManager.getPackageInfo(
                packName, PackageManager.PackageInfoFlags.of(flag.toLong())
            )
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    fun getNameForUid(uid: Int): String? {
        return packageManager.getNameForUid(uid)
    }

    fun getPackageUid(packName: String, flag: Int): Int? {
        return try {
            if (SDK < A13) packageManager.getPackageUid(packName, flag)
            else packageManager.getPackageUid(
                packName, PackageManager.PackageInfoFlags.of(flag.toLong())
            )
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    fun getApplicationInfo(packName: String, flag: Int): ApplicationInfo? {
        return try {
            if (SDK < A13) packageManager.getApplicationInfo(packName, flag)
            else packageManager.getApplicationInfo(
                packName, PackageManager.ApplicationInfoFlags.of(flag.toLong())
            )
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    fun getInstalledPackages(flag: Int): MutableList<PackageInfo> {
        if (SDK < A13) return packageManager.getInstalledPackages(flag)
        return packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(flag.toLong()))
    }

    fun getInstalledApplications(flag: Int): MutableList<ApplicationInfo> {
        return if (SDK < A13) packageManager.getInstalledApplications(flag)
        else packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(flag.toLong()))
    }

    fun resolveActivity(intent: Intent, flag: Int): ResolveInfo? {
        return if (SDK < A13) packageManager.resolveActivity(intent, flag)
        else packageManager.resolveActivity(intent, ResolveInfoFlags.of(flag.toLong()))
    }
}


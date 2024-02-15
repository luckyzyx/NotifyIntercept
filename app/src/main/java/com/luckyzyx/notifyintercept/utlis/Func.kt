package com.luckyzyx.notifyintercept.utlis

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import android.util.Base64
import android.widget.Toast
import com.drake.net.utils.scope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.highcapable.yukihookapi.hook.factory.prefs
import com.luckyzyx.notifyintercept.R
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ShellUtils
import org.json.JSONArray
import org.json.JSONObject

fun Context.removeAppData(jsonObject: JSONObject, packageName: String) {
    val scopes = jsonObject.optJSONArray("scopes")
    if (scopes != null) {
        var index: Int = -1
        for (i in 0 until scopes.length()) {
            val pack = scopes.optJSONObject(i)
            val packName = pack.optString("package")
            if (packName == packageName) index = i
        }
        if (index >= 0) scopes.remove(index)
    } else {
        jsonObject.put("scopes", JSONArray())
    }
    prefs().edit { putString("scopesData", jsonObject.toString()) }
}

fun Context.updateAppData(
    jsonObject: JSONObject, packageName: String, isEnable: Boolean?, niDatas: ArrayList<NotifyInfo>
) {
    val scopes = jsonObject.optJSONArray("scopes")
    if (scopes != null) {
        var index: Int = -1
        for (i in 0 until scopes.length()) {
            val pack = scopes.optJSONObject(i)
            val packName = pack.optString("package")
            if (packName == packageName) index = i
        }
        if (index >= 0) scopes.remove(index)
        scopes.put(JSONObject().apply {
            put("package", packageName)
            put("isEnable", isEnable)
            put("datas", JSONArray().apply {
                niDatas.forEach {
                    put(JSONObject().apply {
                        put("title", it.title)
                        put("content", it.content)
                    })
                }
            })
        })
    } else {
        jsonObject.put("scopes", JSONArray().apply {
            put(JSONObject().apply {
                put("package", packageName)
                put("isEnable", isEnable)
                put("datas", JSONArray().apply {
                    niDatas.forEach {
                        put(JSONObject().apply {
                            put("title", it.title)
                            put("content", it.content)
                        })
                    }
                })
            })
        })
    }
    prefs().edit { putString("scopesData", jsonObject.toString()) }
}

fun isNightMode(configuration: Configuration): Boolean {
    return (configuration.uiMode and 32) > 0
}

fun Context.checkStoragePermission() {
    if (!Environment.isExternalStorageManager()) {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
        startActivity(intent.setData(Uri.parse("package:$packageName")))
    }
}

fun Context.restartAllScope(list: ArrayList<String>?) {
    if (list.isNullOrEmpty()) return
    val commands = ArrayList<String>()
    for (scope in list) {
        if (scope == "android") continue
        if (scope.contains("systemui")) {
            commands.add("kill -9 `pgrep systemui`")
            continue
        }
        commands.add("pkill -9 $scope")
        commands.add("killall $scope")
        commands.add("am force-stop $scope")
    }
    MaterialAlertDialogBuilder(this).apply {
        setMessage(getString(R.string.restart_scope_message))
        setPositiveButton(getString(android.R.string.ok)) { _: DialogInterface?, _: Int ->
            scope {
                com.drake.net.utils.withDefault {
                    if (Shell.getShell().isRoot) ShellUtils.fastCmd(*commands.toTypedArray())
                    else Toast.makeText(context, getString(R.string.no_root), Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
        setNeutralButton(getString(android.R.string.cancel), null)
        show()
    }
}

fun base64Encode(string: String): String {
    return Base64.encodeToString(string.toByteArray(), Base64.DEFAULT)
}

fun base64Decode(string: String): String {
    return String(Base64.decode(string, Base64.DEFAULT))
}
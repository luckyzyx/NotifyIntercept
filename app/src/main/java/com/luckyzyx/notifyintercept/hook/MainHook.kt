package com.luckyzyx.notifyintercept.hook

import android.app.Notification
import android.app.NotificationManager
import android.util.ArraySet
import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.configs
import com.highcapable.yukihookapi.hook.factory.encase
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit
import com.luckyzyx.notifyintercept.utlis.NotifyInfo
import com.luckyzyx.notifyintercept.utlis.safeOfNull
import org.json.JSONArray
import org.json.JSONObject

@InjectYukiHookWithXposed(isUsingResourcesHook = false)
class MainHook : IYukiHookXposedInit {
    override fun onInit() = configs {
        debugLog {
            tag = "NotifyIntercept"
            isEnable = true
            isRecord = true
            elements(TAG, PRIORITY, PACKAGE_NAME, USER_ID)
        }
        isDebug = false
    }

    override fun onHook() = encase {
        loadSystem(Hooker())
        loadApp(packageName, Hooker())
    }
}

@Suppress("DEPRECATION", "UNUSED_VARIABLE")
class Hooker : YukiBaseHooker() {
    override fun onHook() {
        val enableList = prefs.getStringSet("enabledAppList", ArraySet())
        if (!enableList.contains(packageName)) return

        val allNotifyDatas = ArrayList<NotifyInfo>()
        val scopesJson = prefs.getString("scopesData", JSONObject().toString())
        val scopesData = safeOfNull { JSONObject(scopesJson) } ?: JSONObject()
        val scopes = scopesData.optJSONArray("scopes")
        if (scopes != null) {
            var index: Int = -1
            for (i in 0 until scopes.length()) {
                val pack = scopes.optJSONObject(i)
                val packName = pack.optString("package")
                if (packName == packageName) index = i
            }
            if (index >= 0) {
                val pack = scopes.optJSONObject(index)
                val datas = pack.optJSONArray("datas") ?: JSONArray()
                for (i in 0 until datas.length()) {
                    val ni = datas.optJSONObject(i)
                    if (ni != null) {
                        val title = ni.optString("title")
                        val content = ni.optString("content")
                        allNotifyDatas.add(NotifyInfo(title, content))
                    }
                }
            }
        }

        NotificationManager::class.java.apply {
            method { name = "notify";paramCount = 3 }.hook {
                before {
                    val tag = args(0).string()
                    val id = args(1).int()
                    val notify = args(2).cast<Notification>()
                    val bundle = notify?.extras
                    val title = bundle?.get("android.title").toString()
                    val text = bundle?.get("android.text").toString()
                    allNotifyDatas.forEach {
                        if (it.title.isNotBlank() && it.content.isNotBlank()) {
                            if (title.contains(it.title) && text.contains(it.content)) resultNull()
                            return@forEach
                        }
                        if (it.title.isNotBlank()) {
                            if (title.contains(it.title)) resultNull()
                            return@forEach
                        }
                        if (it.content.isNotBlank()) {
                            if (text.contains(it.content)) resultNull()
                            return@forEach
                        }
                    }
                }
            }
        }
    }
}
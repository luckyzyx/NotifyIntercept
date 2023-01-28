package com.luckyzyx.notificationinterception.hook

import android.app.Notification
import android.app.NotificationManager
import android.util.ArraySet
import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.configs
import com.highcapable.yukihookapi.hook.factory.encase
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit

@InjectYukiHookWithXposed(isUsingResourcesHook = false)
class MainHook : IYukiHookXposedInit {
    override fun onInit() = configs {
        debugLog {
            tag = "NI"
            isEnable = true
            isRecord = true
            elements(TAG, PRIORITY, PACKAGE_NAME, USER_ID)
        }
        isDebug = false
        isEnableModulePrefsCache = false
    }

    override fun onHook() = encase {
        loadSystem(Hooker())
        loadApp(packageName, Hooker())
    }
}

@Suppress("UNUSED_VARIABLE", "DEPRECATION")
class Hooker : YukiBaseHooker() {
    override fun onHook() {
        val enableList = prefs.getStringSet("enabledAppList", ArraySet())
        if (!enableList.contains(packageName)) return
        val titleData = prefs.getStringSet(packageName + "_title", ArraySet())
        val textData = prefs.getStringSet(packageName + "_text", ArraySet())
        if (titleData.isEmpty() && textData.isEmpty()) return

        NotificationManager::class.java.hook {
            injectMember {
                method {
                    name = "notify"
                    paramCount = 3
                }
                beforeHook {
                    val tag = args(0).string()
                    val id = args(1).int()
                    val notify = args(2).cast<Notification>()
                    val bundle = notify?.extras
                    val title = bundle?.get("android.title").toString()
                    val text = bundle?.get("android.text").toString()
                    titleData.takeIf { e -> e.isNotEmpty() }?.forEach { if (title.contains(it)) resultNull() }
                    textData.takeIf { e -> e.isNotEmpty() }?.forEach { if (text.contains(it)) resultNull() }
                }
            }
        }
    }
}
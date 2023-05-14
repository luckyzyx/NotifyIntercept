package com.luckyzyx.notifyintercept.hook

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
        val datas = prefs.getStringSet(packageName, ArraySet())
        if (datas.isEmpty()) return

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
                    datas.forEach {
                        val sp = it.split("||")
                        if (sp.size != 2) return@forEach
                        val titleStr = sp[0]
                        val textStr = sp[1]
                        if (titleStr.isNotBlank() && textStr.isNotBlank()) {
                            if (title.contains(titleStr) && text.contains(textStr)) resultNull()
                            return@forEach
                        }
                        if (titleStr.isNotBlank()) {
                            if (title.contains(titleStr)) resultNull()
                            return@forEach
                        }
                        if (textStr.isNotBlank()) {
                            if (text.contains(textStr)) resultNull()
                            return@forEach
                        }
                    }
                }
            }
        }
    }
}
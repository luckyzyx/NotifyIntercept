package com.luckyzyx.notificationinterception.hook

import android.app.Notification
import android.app.NotificationManager
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.loggerD

class Hooker : YukiBaseHooker() {
    override fun onHook() {
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
                    val title = bundle?.get("android.title")
                    val text = bundle?.get("android.text")
                    val packName = packageName

                    loggerD(msg = "\npackName -> $packName\ntag -> $tag\nid -> $id\ntitle -> $title\ntext -> $text")
                }
            }
        }
    }
}
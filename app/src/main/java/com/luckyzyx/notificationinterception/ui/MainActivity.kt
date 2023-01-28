package com.luckyzyx.notificationinterception.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import androidx.appcompat.app.AppCompatActivity
import com.highcapable.yukihookapi.YukiHookAPI
import com.luckyzyx.notificationinterception.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.title = "通知拦截"

        initSkip()
    }

    @SuppressLint("SetTextI18n")
    private fun initSkip() {
        val xposedActive = YukiHookAPI.Status.isXposedModuleActive
        val taichiActive = YukiHookAPI.Status.isTaiChiModuleActive
        val moduleActive = YukiHookAPI.Status.isModuleActive
        binding.activeStatus.apply {
            text = """
                Xposed: $xposedActive
                太极/无极: $taichiActive
                激活状态: $moduleActive
                YukiAPI: ${YukiHookAPI.API_VERSION_NAME}[${YukiHookAPI.API_VERSION_CODE}]
            """.trimIndent()
            gravity = Gravity.CENTER
        }
        binding.mainTv.apply {
            text = """
                通知拦截初版Demo,开发者: 忆清鸣、luckyzyx
                未发布前仅用于测试,未经允许禁止私自分享搬运转发
            """.trimIndent()
        }
        binding.mainBtn.apply {
            text = "配置作用域"
            isEnabled = moduleActive
            setOnClickListener {
                startActivity(Intent(context, AppListActivity::class.java))
            }
        }
    }

}

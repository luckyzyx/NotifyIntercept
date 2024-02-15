package com.luckyzyx.notifyintercept.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import androidx.appcompat.app.AppCompatActivity
import com.highcapable.yukihookapi.YukiHookAPI
import com.highcapable.yukihookapi.YukiHookAPI.VERSION
import com.luckyzyx.notifyintercept.R
import com.luckyzyx.notifyintercept.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.title = getString(R.string.app_name)

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
                ${getString(R.string.taichi_active_status)}: $taichiActive
                ${getString(R.string.module_active_status)}: $moduleActive
                YukiAPI: $VERSION
            """.trimIndent()
            gravity = Gravity.CENTER
        }
        binding.mainTv.apply {
            text = getString(R.string.module_ban_tips)
        }
        binding.mainBtn.apply {
            text = getString(R.string.configuration_scope)
            isEnabled = moduleActive
            setOnClickListener {
                startActivity(Intent(context, AppListActivity::class.java))
            }
        }

//        val intent = Intent(this@MainActivity, AppConfigActivity::class.java)
//        intent.putExtra("packName","com.czy0729.bangumi")
//        startActivity(intent)
    }
}

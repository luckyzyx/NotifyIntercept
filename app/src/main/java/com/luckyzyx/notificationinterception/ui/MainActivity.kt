package com.luckyzyx.notificationinterception.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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
        binding.mainTv.apply {
            text = """
                通知拦截初版Demo,开发者: 忆清鸣、luckyzyx
                未发布前仅用于测试,未经允许禁止私自分享搬运转发
            """.trimIndent()
        }
        binding.mainBtn.apply {
            text = "配置作用域"
            setOnClickListener {
                startActivity(Intent(context, AppListActivity::class.java))
            }
        }
    }

}

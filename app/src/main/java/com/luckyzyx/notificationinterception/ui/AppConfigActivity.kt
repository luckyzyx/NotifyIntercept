package com.luckyzyx.notificationinterception.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.ArraySet
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.highcapable.yukihookapi.hook.factory.modulePrefs
import com.luckyzyx.notificationinterception.databinding.ActivityAppConfigBinding
import com.luckyzyx.notificationinterception.utlis.PackageUtils
import java.util.*

class AppConfigActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppConfigBinding
    private var packName: String = ""
    private var enable: Boolean = false
    private var titleData: Array<String> = arrayOf()
    private var textData: Array<String> = arrayOf()
    private var enabledList = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        initConfig()
    }

    @SuppressLint("SetTextI18n")
    private fun initConfig() {
        val intentPackName = intent?.getStringExtra("packName")
        if (intentPackName == null || intentPackName == "") {
            finish()
        } else packName = intentPackName
        val packInfo = PackageUtils(packageManager).getApplicationInfo(packName, 0)
        supportActionBar?.title = packInfo.loadLabel(packageManager)
        titleData = modulePrefs.getStringSet(packName + "_title", ArraySet()).toTypedArray()
        textData = modulePrefs.getStringSet(packName + "_text", ArraySet()).toTypedArray()
        val getEnabledList = modulePrefs.getStringSet("enabledAppList", ArraySet())
        if (getEnabledList.isNotEmpty()) {
            getEnabledList.forEach {
                enabledList.add(it)
            }
            enable = enabledList.contains(packName)
        }
        binding.enable.apply {
            text = "启用拦截"
            isChecked = enable
            setOnCheckedChangeListener { buttonView, isChecked ->
                if (buttonView.isPressed) enable = isChecked
                binding.dataTitleLayout.isEnabled = enable
                binding.dataTextLayout.isEnabled = enable
            }
        }
        binding.dataTitleLayout.isEnabled = enable
        binding.dataTextLayout.isEnabled = enable

        binding.dataTitle.apply {
            var string = ""
            if (titleData.isNotEmpty()) titleData.forEach {
                if (it.isNotBlank()) string += "\n$it"
            }
            if (string.isNotBlank()) string = string.substring(1, string.length)
            setText(string)
        }
        binding.dataText.apply {
            var string = ""
            if (textData.isNotEmpty()) textData.forEach {
                if (it.isNotBlank()) string += "\n$it"
            }
            if (string.isNotBlank()) string = string.substring(1, string.length)
            setText(string)
        }

        binding.dataTip.apply {
            text = "字符务必按行填写,否则无法进行匹配,通知内容为可选项,不使用时请保持为空\n\n" +
                    "根据输入的字符与通知进行对比拦截,当通知包含此关键词或句子时会进行拦截\n\n" +
                    "字符长度较短有可能会拦截掉其他通知,务必输入正确且完整的通知文字\n\n" +
                    "注: 切换系统语言后必须重新设置拦截关键词,否则可能会失效"
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.add(0, 1, 0, "保存")?.apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        if (item.itemId == 1) {
            if (enable) {
                if (!enabledList.contains(packName)) enabledList.add(packName)
            } else if (enabledList.contains(packName)) enabledList.remove(packName)
            modulePrefs.putStringSet("enabledAppList", enabledList.toSet())
            val titles = if (binding.dataTitle.text.toString().isBlank()) {
                ArrayList()
            } else binding.dataTitle.text.toString().trim().split("\n")
            val titledata = ArrayList<String>().apply {
                if (titles.isNotEmpty()) titles.forEach {
                    if (it.isNotBlank()) this.add(it)
                }
            }
            modulePrefs.putStringSet(packName + "_title", titledata.toSet())
            val texts = if (binding.dataText.text.toString().isBlank()) {
                ArrayList()
            } else binding.dataText.text.toString().trim().split("\n")
            val textdata = ArrayList<String>().apply {
                if (texts.isNotEmpty()) texts.forEach {
                    if (it.isNotBlank()) this.add(it)
                }
            }
            modulePrefs.putStringSet(packName + "_text", textdata.toSet())
        }
        return super.onOptionsItemSelected(item)
    }
}
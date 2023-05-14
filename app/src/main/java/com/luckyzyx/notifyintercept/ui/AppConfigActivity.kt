package com.luckyzyx.notifyintercept.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.ArraySet
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.highcapable.yukihookapi.hook.factory.prefs
import com.luckyzyx.notifyintercept.R
import com.luckyzyx.notifyintercept.databinding.ActivityAppConfigBinding
import com.luckyzyx.notifyintercept.utlis.PackageUtils
import java.util.*

class AppConfigActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppConfigBinding
    private var packName: String = ""
    private var enable: Boolean = false
    private var enabledList = ArrayList<String>()

    private var appConfigAdapter: AppConfigAdapter? = null
    private var allNIdatas = ArrayList<NIInfo>()

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
        if (intent == null || intentPackName == null || intentPackName == "") {
            finish()
        } else packName = intentPackName
        val packInfo = PackageUtils(packageManager).getApplicationInfo(packName, 0)
        supportActionBar?.title = packInfo.loadLabel(packageManager)

        val getEnabledList = prefs().getStringSet("enabledAppList", ArraySet())
        if (getEnabledList.isNotEmpty()) {
            getEnabledList.forEach {
                enabledList.add(it)
            }
            enable = enabledList.contains(packName)
        }
        binding.niEnable.apply {
            text = getString(R.string.ni_enable_intercept)
            isChecked = enable
            setOnCheckedChangeListener { buttonView, isChecked ->
                if (buttonView.isPressed) enable = isChecked
                if (enable) {
                    if (!enabledList.contains(packName)) enabledList.add(packName)
                } else if (enabledList.contains(packName)) enabledList.remove(packName)
                prefs().edit { putStringSet("enabledAppList", enabledList.toSet()) }
            }
        }

        binding.niSwipeRefreshLayout.apply {
            setOnRefreshListener { loadData() }
        }
        if (allNIdatas.isEmpty()) loadData()

        binding.niAddData.apply {
            setOnClickListener {
                val addDialog = MaterialAlertDialogBuilder(
                    context,
                    com.google.android.material.R.style.MaterialAlertDialog_Material3_Title_Text_CenterStacked
                ).apply {
                    setTitle(getString(R.string.ni_add_notify))
                    setView(R.layout.layout_ni_dialog)
                }.show()
                val titleView = addDialog.findViewById<TextInputEditText>(R.id.data_title)
                val textView = addDialog.findViewById<TextInputEditText>(R.id.data_text)
                addDialog.findViewById<MaterialButton>(R.id.data_save)?.apply {
                    setOnClickListener {
                        val titleStr = titleView?.text.toString()
                        val textStr = textView?.text.toString()
                        if (titleStr.isNotBlank() || textStr.isNotBlank()) {
                            appConfigAdapter?.addData(
                                NIInfo(titleStr, textStr)
                            )
                            addDialog.dismiss()
                        }
                    }
                }
            }
        }

        binding.niTip.apply {
            text =
                getString(R.string.ni_data_tips_1) + getString(R.string.ni_data_tips_2) + getString(
                    R.string.ni_data_tips_3
                )
        }
    }

    private fun loadData() {
        binding.niSwipeRefreshLayout.isRefreshing = true
        allNIdatas.clear()
        val getData = prefs().getStringSet(packName, ArraySet()).toTypedArray()
        getData.takeIf { e -> e.isNotEmpty() }?.forEach {
            val sp = it.split("||")
            if (sp.size == 2) {
                val title = sp[0]
                val text = sp[1]
                allNIdatas.add(NIInfo(title, text))
            }
        }
        appConfigAdapter = AppConfigAdapter(this@AppConfigActivity, packName, allNIdatas)
        binding.niRecyclerView.apply {
            adapter = appConfigAdapter
            layoutManager = LinearLayoutManager(this@AppConfigActivity)
        }
        binding.niSwipeRefreshLayout.isRefreshing = false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }
}
package com.luckyzyx.notifyintercept.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.drake.net.utils.scopeLife
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.highcapable.yukihookapi.hook.factory.prefs
import com.luckyzyx.notifyintercept.R
import com.luckyzyx.notifyintercept.databinding.ActivityAppConfigBinding
import com.luckyzyx.notifyintercept.databinding.DialogNotifyInfoBinding
import com.luckyzyx.notifyintercept.utlis.NotifyInfo
import com.luckyzyx.notifyintercept.utlis.PackageUtils
import com.luckyzyx.notifyintercept.utlis.safeOfNull
import com.luckyzyx.notifyintercept.utlis.updateAppData
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class AppConfigActivity : AppCompatActivity() {
    private val tags = "AppConfigActivity"

    private lateinit var binding: ActivityAppConfigBinding

    private var packName: String = ""
    private var enabled: Boolean = false
    private var allNotifyDatas = ArrayList<NotifyInfo>()

    private var appConfigAdapter: AppConfigAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initConfig()
    }

    @SuppressLint("SetTextI18n")
    private fun initConfig() {
        allNotifyDatas.clear()
        if (intent == null) finish()

        val scopeJson = prefs().getString("scopesData", JSONObject().toString())
        val scopesData = safeOfNull { JSONObject(scopeJson) } ?: JSONObject()
//        LogUtils.d(tags, "initConfig", "${scopesData.toString()}", true)

        packName = intent.getStringExtra("packName") ?: ""
        if (packName.isBlank()) finish()
//        LogUtils.d(tags, "initConfig", "${packName.toString()}", true)

        enabled = intent.getBooleanExtra("isEnable", false)
//        LogUtils.d(tags, "initConfig", "${enabled.toString()}", true)

        val jsonArr = intent.getStringExtra("datas") ?: JSONArray().toString()
//        LogUtils.d(tags, "initConfig", "${jsonArr.toString()}", true)

        val notifyDatas = safeOfNull { JSONArray(jsonArr) } ?: JSONArray()
        for (i in 0 until notifyDatas.length()) {
            val ni = notifyDatas.optJSONObject(i)
            if (ni != null) {
                val title = ni.optString("title")
                val content = ni.optString("content")
                allNotifyDatas.add(NotifyInfo(title, content))
            }
        }

        val appinfo = PackageUtils(packageManager).getApplicationInfo(packName, 0)
        supportActionBar?.title = appinfo?.loadLabel(packageManager)

        binding.niEnable.apply {
            text = getString(R.string.ni_enable_intercept)
            isChecked = enabled
            setOnCheckedChangeListener { buttonView, isChecked ->
                if (!buttonView.isPressed) return@setOnCheckedChangeListener
                enabled = isChecked
                updateAppData(scopesData, packName, isChecked, allNotifyDatas)
            }
        }

        binding.niSwipeRefreshLayout.apply {
            setOnRefreshListener { loadData() }
        }

        binding.niAddData.apply {
            setOnClickListener {
                val dialogBinding = DialogNotifyInfoBinding.inflate(layoutInflater)
                val addDialog = MaterialAlertDialogBuilder(
                    context,
                    com.google.android.material.R.style.MaterialAlertDialog_Material3_Title_Text_CenterStacked
                ).apply {
                    setTitle(getString(R.string.ni_add_notify))
                    setView(dialogBinding.root)
                }.show()
                val titleView = dialogBinding.dataTitle
                val contentView = dialogBinding.dataContent

                dialogBinding.dataSave.apply {
                    setOnClickListener {
                        val titleStr = titleView.text.toString()
                        val textStr = contentView.text.toString()
                        if (titleStr.isNotBlank() || textStr.isNotBlank()) {
                            appConfigAdapter?.addData(NotifyInfo(titleStr, textStr))
                            addDialog.dismiss()
                        }
                    }
                }
            }
        }

        binding.niTip.apply {
            text = getString(R.string.ni_data_tips_1) +
                    getString(R.string.ni_data_tips_2) +
                    getString(R.string.ni_data_tips_3)
        }

        loadData()
    }

    private fun loadData() {
        scopeLife {
            binding.niSwipeRefreshLayout.isRefreshing = true
            binding.niRecyclerView.apply {
                appConfigAdapter = AppConfigAdapter(
                    context, packName, enabled, allNotifyDatas
                )
                adapter = appConfigAdapter
                layoutManager = LinearLayoutManager(context)
            }
            binding.niSwipeRefreshLayout.isRefreshing = false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }
}
package com.luckyzyx.notificationinterception.ui

import android.content.pm.ApplicationInfo
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.drake.net.utils.scopeLife
import com.drake.net.utils.withIO
import com.highcapable.yukihookapi.hook.factory.modulePrefs
import com.luckyzyx.notificationinterception.R
import com.luckyzyx.notificationinterception.databinding.ActivityAppListBinding
import com.luckyzyx.notificationinterception.utlis.PackageUtils

class AppListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppListBinding
    private var appListAllDatas = ArrayList<AppInfo>()
    private var appListAdapter: AppListAdapter? = null
    private var isShowSystemApp: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "作用域"
        initView()
    }

    private fun initView() {
        isShowSystemApp = modulePrefs.getBoolean("show_system_app", false)
        binding.searchViewLayout.apply {
            hint = "Name / PackageName"
            isHintEnabled = true
            isHintAnimationEnabled = true
        }
        binding.searchView.apply {
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    appListAdapter?.getFilter?.filter(s.toString())
                }

                override fun afterTextChanged(s: Editable?) {}
            })
        }
        binding.swipeRefreshLayout.apply {
            setOnRefreshListener {
                loadData()
            }
        }
        if (appListAllDatas.isEmpty()) loadData()
    }

    private fun loadData() {
        binding.swipeRefreshLayout.isRefreshing = true
        binding.searchViewLayout.isEnabled = false
        binding.searchView.text = null
        appListAllDatas.clear()
        scopeLife {
            withIO {
                val appinfos = PackageUtils(packageManager).getInstalledApplications(0)
                for (i in appinfos) {
                    if (i.flags and ApplicationInfo.FLAG_SYSTEM == 1 && !isShowSystemApp) continue
                    if (i.packageName == packageName) continue
                    appListAllDatas.add(
                        AppInfo(
                            i.loadIcon(packageManager),
                            i.loadLabel(packageManager),
                            i.packageName,
                        )
                    )
                }
            }
            appListAdapter = AppListAdapter(this@AppListActivity, appListAllDatas)
            binding.recyclerView.apply {
                adapter = appListAdapter
                layoutManager = LinearLayoutManager(this@AppListActivity)
            }
            binding.swipeRefreshLayout.isRefreshing = false
            binding.searchViewLayout.isEnabled = true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        menu.findItem(R.id.show_system_app).apply {
            isChecked = isShowSystemApp
            if (isNightMode(resources.configuration)) {
                iconTintList = ColorStateList.valueOf(Color.WHITE)
            }
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        if (item.itemId == R.id.show_system_app) {
            item.isChecked = !item.isChecked
            isShowSystemApp = item.isChecked
            modulePrefs.putBoolean("show_system_app", isShowSystemApp)
            loadData()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun isNightMode(configuration: Configuration): Boolean {
        return (configuration.uiMode and 32) > 0
    }

    override fun onRestart() {
        super.onRestart()
        loadData()
    }
}
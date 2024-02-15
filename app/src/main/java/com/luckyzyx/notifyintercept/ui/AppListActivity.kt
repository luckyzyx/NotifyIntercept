package com.luckyzyx.notifyintercept.ui

import android.content.pm.ApplicationInfo
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.drake.net.utils.scopeLife
import com.drake.net.utils.withDefault
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.highcapable.yukihookapi.hook.factory.prefs
import com.luckyzyx.notifyintercept.R
import com.luckyzyx.notifyintercept.databinding.ActivityAppListBinding
import com.luckyzyx.notifyintercept.utlis.AppInfo
import com.luckyzyx.notifyintercept.utlis.PackageUtils
import com.luckyzyx.notifyintercept.utlis.base64Decode
import com.luckyzyx.notifyintercept.utlis.base64Encode
import com.luckyzyx.notifyintercept.utlis.checkStoragePermission
import com.luckyzyx.notifyintercept.utlis.isNightMode
import com.luckyzyx.notifyintercept.utlis.removeAppData
import com.luckyzyx.notifyintercept.utlis.restartAllScope
import com.luckyzyx.notifyintercept.utlis.safeOfNull
import org.json.JSONObject
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppListBinding

    private var allAppInfos = ArrayList<AppInfo>()
    private var appListAdapter: AppListAdapter? = null
    private var isShowSystemApp: Boolean = false

    private lateinit var scopesData: JSONObject

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.app_list_title)

        initView()
    }

    private fun initView() {
        isShowSystemApp = prefs().getBoolean("show_system_app", false)
        binding.appSearchViewLayout.apply {
            hint = "Name / PackageName"
            isHintEnabled = true
            isHintAnimationEnabled = true
        }
        binding.appSearchView.apply {
            addTextChangedListener(onTextChanged = { text: CharSequence?, _: Int, _: Int, _: Int ->
                appListAdapter?.getFilter?.filter(text.toString())
            })
        }
        binding.appSwipeRefreshLayout.apply {
            setOnRefreshListener { loadData() }
        }
        if (allAppInfos.isEmpty()) loadData()
    }

    private fun loadData() {
        scopeLife {
            binding.appSwipeRefreshLayout.isRefreshing = true
            binding.appSearchViewLayout.isEnabled = false
            binding.appSearchView.text = null
            allAppInfos.clear()

            val scopeJson = prefs().getString("scopesData", JSONObject().toString())
            scopesData = safeOfNull { JSONObject(scopeJson) } ?: JSONObject()

            withDefault {
                val appinfos = PackageUtils(packageManager).getInstalledApplications(0)
                for (i in appinfos) {
                    if (i.flags and ApplicationInfo.FLAG_SYSTEM == 1 && !isShowSystemApp) continue
                    if (i.packageName == packageName) continue
                    allAppInfos.add(
                        AppInfo(
                            i.loadIcon(packageManager),
                            i.loadLabel(packageManager),
                            i.packageName,
                        )
                    )
                }
            }

            binding.appRecyclerView.apply {
                appListAdapter = AppListAdapter(context, allAppInfos)
                adapter = appListAdapter
                layoutManager = LinearLayoutManager(context)
            }
            binding.appSwipeRefreshLayout.isRefreshing = false
            binding.appSearchViewLayout.isEnabled = true
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
        menu.add(0, 1, 0, "Restart Menu").apply {
            setIcon(R.drawable.baseline_refresh_24)
            setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            if ((resources.configuration.uiMode and 32) > 0) {
                iconTintList = ColorStateList.valueOf(Color.WHITE)
            }
        }
        menu.add(0, 2, 0, "Clear UnUsed").apply {
            setIcon(R.drawable.baseline_cleaning_services_24)
            setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            if ((resources.configuration.uiMode and 32) > 0) {
                iconTintList = ColorStateList.valueOf(Color.WHITE)
            }
        }
        menu.add(0, 3, 0, getString(R.string.data_import_and_export)).apply {
            setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.show_system_app -> {
                item.isChecked = !item.isChecked
                isShowSystemApp = item.isChecked
                prefs().edit { putBoolean("show_system_app", isShowSystemApp) }
                loadData()
            }

            1 -> restartAllScope(appListAdapter?.getEnabledApps())
            2 -> MaterialAlertDialogBuilder(
                this,
                com.google.android.material.R.style.MaterialAlertDialog_Material3_Title_Text_CenterStacked
            ).apply {
                setMessage(getString(R.string.clear_unused_rules))
                setPositiveButton(android.R.string.ok) { _, _ ->
                    appListAdapter?.apply {
                        val enabled = getEnabledApps()
                        getAppDatas().keys.forEachIndexed { _, s ->
                            if (enabled.contains(s).not()) removeAppData(scopesData, s)
                        }
                        loadData()
                    }
                }
                setNeutralButton(android.R.string.cancel, null)
            }.show()

            3 -> datasDialog()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onRestart() {
        super.onRestart()
        loadData()
    }

    private val backupData =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {
            if (it != null) {
                writeBackupData(it)
            }
        }
    private val restoreData =
        registerForActivityResult(ActivityResultContracts.GetContent()) {
            if (it != null) {
                writeRestoreData(readFromUri(it))
                finish()
            }
        }

    private fun datasDialog() {
        val list = arrayOf(getString(R.string.data_import), getString(R.string.data_export))
        MaterialAlertDialogBuilder(
            this,
            com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
        ).apply {
            setItems(list) { _, which ->
                when (which) {
                    0 -> {
                        checkStoragePermission()
                        restoreData.launch("application/json")
                    }

                    1 -> {
                        checkStoragePermission()
                        val date = SimpleDateFormat(
                            "yyyyMMdd_HHmmss", Locale.getDefault()
                        ).format(Date())
                        val fileName = "NI_" + date + "_backup.json"
                        backupData.launch(fileName)
                    }
                }
            }
            show()
        }
    }

    private fun writeBackupData(uri: Uri) {
        val str = base64Encode(scopesData.toString())
        try {
            contentResolver.openFileDescriptor(uri, "w")?.use { its ->
                FileOutputStream(its.fileDescriptor).use {
                    it.write(str.toByteArray())
                }
            }
            //context.toast(getString(R.string.data_backup_complete))
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            //context.toast(getString(R.string.data_backup_error))
        } catch (e: IOException) {
            e.printStackTrace()
            //context.toast(getString(R.string.data_backup_error))
        }
    }

    private fun writeRestoreData(data: String) {
        val json = safeOfNull { JSONObject(base64Decode(data)) }
        if (json != null) prefs().edit { putString("scopesData", json.toString()) }
    }

    private fun readFromUri(uri: Uri): String {
        val stringBuilder = StringBuilder()
        contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    stringBuilder.append(line)
                    line = reader.readLine()
                }
            }
        }
        return stringBuilder.toString()
    }
}
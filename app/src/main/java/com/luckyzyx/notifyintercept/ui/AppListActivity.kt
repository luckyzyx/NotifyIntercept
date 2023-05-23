package com.luckyzyx.notifyintercept.ui

import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.ArraySet
import android.util.Base64
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.drake.net.utils.scope
import com.drake.net.utils.scopeLife
import com.drake.net.utils.withDefault
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.highcapable.yukihookapi.hook.factory.prefs
import com.luckyzyx.notifyintercept.R
import com.luckyzyx.notifyintercept.databinding.ActivityAppListBinding
import com.luckyzyx.notifyintercept.utlis.PackageUtils
import com.luckyzyx.notifyintercept.utlis.ShellUtils
import org.json.JSONArray
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
    private var appListAllDatas = ArrayList<AppInfo>()
    private var appListAdapter: AppListAdapter? = null
    private var isShowSystemApp: Boolean = false
    private var enableData: Set<String>? = null

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
        binding.appSwipeRefreshLayout.apply {
            setOnRefreshListener { loadData() }
        }
        if (appListAllDatas.isEmpty()) loadData()
    }

    private fun loadData() {
        scopeLife {
            binding.appSwipeRefreshLayout.isRefreshing = true
            binding.appSearchViewLayout.isEnabled = false
            binding.appSearchView.text = null
            appListAllDatas.clear()
            enableData = prefs().getStringSet("enabledAppList", ArraySet())
            withDefault {
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
            appListAdapter = AppListAdapter(this@AppListActivity, appListAllDatas, enableData)
            binding.appRecyclerView.apply {
                adapter = appListAdapter
                layoutManager = LinearLayoutManager(this@AppListActivity)
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
        menu.add(0, 2, 0, getString(R.string.data_import_and_export)).apply {
            setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        if (item.itemId == R.id.show_system_app) {
            item.isChecked = !item.isChecked
            isShowSystemApp = item.isChecked
            prefs().edit { putBoolean("show_system_app", isShowSystemApp) }
            loadData()
        }
        if (item.itemId == 1) restartAllScope(enableData)
        if (item.itemId == 2) datasDialog()
        return super.onOptionsItemSelected(item)
    }

    private fun isNightMode(configuration: Configuration): Boolean {
        return (configuration.uiMode and 32) > 0
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
                        checkPermission()
                        restoreData.launch("application/json")
                    }

                    1 -> {
                        checkPermission()
                        val date =
                            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                        val fileName =
                            "NI_" + date + "_backup.json"
                        backupData.launch(fileName)
                    }
                }
            }
            show()
        }
    }

    private fun writeBackupData(uri: Uri) {
        val json = JSONObject()
        val datas = prefs().all()
        datas.keys.forEach { key ->
            datas[key].apply {
                if (this?.javaClass?.simpleName == "HashSet") {
                    val arr = JSONArray()
                    val value = (this as HashSet<*>).toTypedArray()
                    for (i in value.indices) {
                        arr.put(value[i])
                    }
                    json.put(key, arr)
                } else {
                    json.put(key, this)
                }
            }
        }
        val str = base64Encode(json.toString())
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
        val json = JSONObject(base64Decode(data))
        if (json.length() <= 0) return

        if (json.length() > 0) {
            json.keys().forEach { key ->
                val value = json.get(key)
                when (value.javaClass.simpleName) {
                    "Boolean" -> prefs().edit { putBoolean(key, value as Boolean) }
                    "Integer" -> prefs().edit { putInt(key, value as Int) }
                    "JSONArray" -> {
                        val set = ArraySet<String>()
                        val list = value as JSONArray
                        for (i in 0 until list.length()) {
                            set.add(list[i] as String)
                        }
                        prefs().edit { putStringSet(key, set) }
                    }

                    "String" -> prefs().edit { putString(key, value as String) }
                    else -> Toast.makeText(this, "Error: $key", Toast.LENGTH_SHORT).show()
                }
            }
        }
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

    private fun base64Encode(string: String): String {
        return Base64.encodeToString(string.toByteArray(), Base64.DEFAULT)
    }

    private fun base64Decode(string: String): String {
        return String(Base64.decode(string, Base64.DEFAULT))
    }

    private fun restartAllScope(list: Set<String>?) {
        if (list == null) return
        val commands = ArrayList<String>()
        for (scope in list) {
            if (scope == "android") continue
            if (scope.contains("systemui")) {
                commands.add("kill -9 `pgrep systemui`")
                continue
            }
            commands.add("killall $scope")
            commands.add("am force-stop $scope")
        }
        MaterialAlertDialogBuilder(this).apply {
            setMessage(getString(R.string.restart_scope_message))
            setPositiveButton(getString(android.R.string.ok)) { _: DialogInterface?, _: Int ->
                scope {
                    withDefault {
                        ShellUtils.execCommand(commands, true)
                    }
                }
            }
            setNeutralButton(getString(android.R.string.cancel), null)
            show()
        }
    }

    private fun checkPermission() {
        if (!Environment.isExternalStorageManager()) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            startActivity(intent.setData(Uri.parse("package:$packageName")))
        }
    }
}
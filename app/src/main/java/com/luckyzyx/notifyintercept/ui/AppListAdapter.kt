package com.luckyzyx.notifyintercept.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.ArrayMap
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.highcapable.yukihookapi.hook.factory.prefs
import com.luckyzyx.notifyintercept.databinding.LayoutAppinfoItemBinding
import com.luckyzyx.notifyintercept.utlis.AppInfo
import com.luckyzyx.notifyintercept.utlis.safeOfNull
import org.json.JSONArray
import org.json.JSONObject

class AppListAdapter(
    private val context: Context, private val allAppInfos: ArrayList<AppInfo>
) : RecyclerView.Adapter<AppListAdapter.ViewHolder>() {

    private var allDatas = ArrayList<AppInfo>()
    private var filterDatas = ArrayList<AppInfo>()

    private val enabeldApp = ArrayList<String>()
    private val appData = ArrayMap<String, JSONArray>()

    init {
        initDatas()
    }

    private fun initDatas() {
        allDatas.clear()
        filterDatas.clear()
        enabeldApp.clear()
        appData.clear()

        allDatas = allAppInfos.apply {
            sortBy { it.appName.toString() }
        }

        val scopeJson = context.prefs().getString("scopesData", JSONObject().toString())
        val scopesData = safeOfNull { JSONObject(scopeJson) } ?: JSONObject()
        val scopes = scopesData.optJSONArray("scopes")
        if (scopes != null) {
            for (i in 0 until scopes.length()) {
                val pack = scopes.optJSONObject(i)
                if (pack != null) {
                    val packName = pack.optString("package")
                    val enabled = pack.optBoolean("isEnable")
                    val data = pack.optJSONArray("datas")
                    if (packName.isNotBlank()) {
                        if (enabled) enabeldApp.add(packName)
                        appData[packName] = data
                    }
                }
            }
            context.prefs().edit { putStringSet("enabledAppList", enabeldApp.toSet()) }
        }

        val sortDatas = ArrayList<AppInfo>()
        allDatas.apply {
            forEach { its ->
                if (enabeldApp.contains(its.packName)) sortDatas.add(its)
            }
            removeIf { sortDatas.contains(it) }
            addAll(0, sortDatas.apply {
                sortBy { it.appName.toString() }
            })
        }
        filterDatas = allDatas
        refreshDatas()
    }

    class ViewHolder(binding: LayoutAppinfoItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val appInfoView: MaterialCardView = binding.root
        val appIcon: ImageView = binding.appIcon
        val appName: TextView = binding.appName
        val packName: TextView = binding.packName
        val dataCount: TextView = binding.dataCount
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = LayoutAppinfoItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return filterDatas.size
    }

    fun getEnabledApps(): ArrayList<String> {
        initDatas()
        return enabeldApp
    }

    fun getAppDatas(): ArrayMap<String, JSONArray> {
        initDatas()
        return appData
    }

    val getFilter = object : Filter() {
        override fun performFiltering(constraint: CharSequence): FilterResults {
            filterDatas = if (constraint.isBlank()) {
                allDatas
            } else {
                val filterlist = ArrayList<AppInfo>()
                for (data in allDatas) {
                    if (
                        data.appName.toString().lowercase()
                            .contains(constraint.toString().lowercase()) ||
                        data.packName.lowercase().contains(constraint.toString().lowercase())
                    ) {
                        filterlist.add(data)
                    }
                }
                filterlist
            }
            val filterResults = FilterResults()
            filterResults.values = filterDatas
            return filterResults
        }

        override fun publishResults(constraint: CharSequence, results: FilterResults?) {
            @Suppress("UNCHECKED_CAST")
            filterDatas = results?.values as ArrayList<AppInfo>
            refreshDatas()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun refreshDatas() {
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appIcon = filterDatas[position].appIcon
        val appName = filterDatas[position].appName
        val packName = filterDatas[position].packName
        val datas = appData[packName] ?: JSONArray()
        holder.appIcon.setImageDrawable(null)
        holder.appIcon.setImageDrawable(appIcon)
        holder.appName.text = null
        holder.appName.text = appName
        holder.packName.text = null
        holder.packName.text = packName
        holder.dataCount.text = null
        if (datas.length() > 0) holder.dataCount.text = datas.length().toString()
        holder.appInfoView.setOnClickListener(null)
        holder.appInfoView.setCardBackgroundColor(Color.WHITE)

        val isEnable = enabeldApp.contains(packName)
        if (isEnable) holder.appInfoView.setCardBackgroundColor(Color.parseColor("#D4E4E4"))

        holder.appInfoView.setOnClickListener {
            Intent(context, AppConfigActivity::class.java).apply {
                putExtra("packName", packName)
                putExtra("isEnable", isEnable)
                if (datas.length() > 0) putExtra("datas", datas.toString())
                context.startActivity(this)
            }
        }
    }
}
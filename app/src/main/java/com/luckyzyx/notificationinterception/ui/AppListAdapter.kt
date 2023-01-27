package com.luckyzyx.notificationinterception.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.ArraySet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.highcapable.yukihookapi.hook.factory.modulePrefs
import com.luckyzyx.notificationinterception.databinding.LayoutAppinfoItemBinding
import java.io.Serializable

data class AppInfo(
    var appIcon: Drawable,
    var appName: CharSequence,
    var packName: String,
) : Serializable

class AppListAdapter(private val context: Context, datas: ArrayList<AppInfo>) :
    RecyclerView.Adapter<AppListAdapter.ViewHolder>() {

    private var allDatas = ArrayList<AppInfo>()
    private var filterDatas = ArrayList<AppInfo>()
    private var enabledList = ArrayList<String>()
    private var sortData = ArrayList<AppInfo>()

    init {
        allDatas = datas
        sortDatas()
        filterDatas = datas
    }

    private fun sortDatas() {
        val getEnabledList = context.modulePrefs.getStringSet("enabledAppList", ArraySet())
        if (getEnabledList.isNotEmpty()) {
            for (i in getEnabledList) {
                enabledList.add(i)
            }
        }
        allDatas.forEach { its ->
            if (enabledList.contains(its.packName)) sortData.add(0, its)
        }
        enabledList.clear()
        sortData.forEach {
            enabledList.add(it.packName)
        }
        context.modulePrefs.putStringSet("enabledAppList", enabledList.toSet())
        allDatas.apply {
            sortData.forEach {
                this.remove(it)
                this.add(0, it)
            }
        }
    }

    class ViewHolder(binding: LayoutAppinfoItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val appInfoView: MaterialCardView = binding.root
        val appIcon: ImageView = binding.appIcon
        val appName: TextView = binding.appName
        val packName: TextView = binding.packName
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            LayoutAppinfoItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return filterDatas.size
    }

    val getFilter
        get() = object : Filter() {
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

            @SuppressLint("NotifyDataSetChanged")
            override fun publishResults(constraint: CharSequence, results: FilterResults?) {
                @Suppress("UNCHECKED_CAST")
                filterDatas = results?.values as ArrayList<AppInfo>
                notifyDataSetChanged()
            }
        }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.appIcon.setImageDrawable(filterDatas[position].appIcon)
        holder.appName.text = filterDatas[position].appName
        holder.packName.text = filterDatas[position].packName
        holder.appInfoView.setOnClickListener(null)

        val isEnable = enabledList.contains(filterDatas[position].packName)
        if (isEnable) holder.appInfoView.setCardBackgroundColor(Color.parseColor("#D4E4E4"))
        holder.appInfoView.setOnClickListener {
            val intent = Intent(context, AppConfigActivity::class.java)
            intent.putExtra("packName",filterDatas[position].packName)
            context.startActivity(intent)
        }
    }

}
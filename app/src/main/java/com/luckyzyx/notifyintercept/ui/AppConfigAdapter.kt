package com.luckyzyx.notifyintercept.ui

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.highcapable.yukihookapi.hook.factory.prefs
import com.luckyzyx.notifyintercept.R
import com.luckyzyx.notifyintercept.databinding.DialogNotifyInfoBinding
import com.luckyzyx.notifyintercept.databinding.LayoutNotifyItemBinding
import com.luckyzyx.notifyintercept.utlis.NotifyInfo
import com.luckyzyx.notifyintercept.utlis.safeOfNull
import com.luckyzyx.notifyintercept.utlis.updateAppData
import org.json.JSONObject

class AppConfigAdapter(
    private val context: Context, private val packName: String,
    private val enabled: Boolean, datas: ArrayList<NotifyInfo>
) :
    RecyclerView.Adapter<AppConfigAdapter.ViewHolder>() {

    private var scopesData = JSONObject()
    private var allDatas = ArrayList<NotifyInfo>()

    init {
        initData()
        allDatas = datas
    }

    private fun initData() {
        val scopeJson = context.prefs().getString("scopesData", JSONObject().toString())
        scopesData = safeOfNull { JSONObject(scopeJson) } ?: JSONObject()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = LayoutNotifyItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val titleStr = allDatas[position].title
        val contentStr = allDatas[position].content

        holder.cardView.apply {
            holder.cardView.setOnClickListener(null)
            holder.cardView.setOnLongClickListener(null)
            setOnClickListener {
                val dialogBinding = DialogNotifyInfoBinding.inflate(LayoutInflater.from(context))
                val editDialog = MaterialAlertDialogBuilder(
                    context,
                    com.google.android.material.R.style.MaterialAlertDialog_Material3_Title_Text_CenterStacked
                ).apply {
                    setTitle(context.getString(R.string.ni_edit_notify))
                    setView(dialogBinding.root)
                }.show()
                val titleView = dialogBinding.dataTitle.apply {
                    setText(titleStr)
                }
                val textView = dialogBinding.dataContent.apply {
                    setText(contentStr)
                }
                dialogBinding.dataSave.apply {
                    setOnClickListener {
                        val title = titleView.text.toString()
                        val text = textView.text.toString()
                        if (title.isNotBlank() || text.isNotBlank()) {
                            allDatas[position] = NotifyInfo(title, text)
                            saveAllData()
                            editDialog.dismiss()
                        }
                    }
                }
            }
            setOnLongClickListener {
                MaterialAlertDialogBuilder(
                    context,
                    com.google.android.material.R.style.MaterialAlertDialog_Material3_Title_Text_CenterStacked
                ).apply {
                    setTitle(context.getString(R.string.dialog_remove_tips))
                    setPositiveButton(android.R.string.ok) { _, _ ->
                        allDatas.removeAt(position)
                        saveAllData()
                    }
                    setNeutralButton(android.R.string.cancel, null)
                }.show()
                true
            }
        }

        holder.title.apply {
            setOnClickListener(null)
            setOnLongClickListener(null)
            setOnClickListener {
                holder.cardView.performClick()
            }
            setOnLongClickListener {
                holder.cardView.performLongClick()
                true
            }
            text = "${context.getString(R.string.ni_title)}: $titleStr"
            isVisible = titleStr.isNotBlank()
        }
        holder.content.apply {
            setOnClickListener(null)
            setOnLongClickListener(null)
            setOnClickListener {
                holder.cardView.performClick()
            }
            setOnLongClickListener {
                holder.cardView.performLongClick()
                true
            }
            text = "${context.getString(R.string.ni_content)}: $contentStr"
            isVisible = contentStr.isNotBlank()
        }
    }

    override fun getItemCount(): Int {
        return allDatas.size
    }

    fun addData(data: NotifyInfo) {
        allDatas.add(data)
        saveAllData()
    }

    private fun saveAllData() {
        context.updateAppData(scopesData, packName, enabled, allDatas)
        refreshDatas()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun refreshDatas() {
        notifyDataSetChanged()
    }

    class ViewHolder(binding: LayoutNotifyItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val cardView = binding.root
        val title = binding.dataTitle
        val content = binding.dataContent
    }
}
package com.luckyzyx.notifyintercept.ui

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.highcapable.yukihookapi.hook.factory.modulePrefs
import com.luckyzyx.notifyintercept.R
import com.luckyzyx.notifyintercept.databinding.LayoutNiItemBinding
import java.io.Serializable

data class NIInfo(
    val title: String,
    val text: String
) : Serializable

class AppConfigAdapter(
    private val context: Context,
    private val packName: String,
    datas: ArrayList<NIInfo>
) :
    RecyclerView.Adapter<AppConfigAdapter.ViewHolder>() {

    private var allDatas = ArrayList<NIInfo>()

    init {
        allDatas = datas
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            LayoutNiItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val titleStr = allDatas[position].title
        val textStr = allDatas[position].text
        holder.title.apply {
            text = "${context.getString(R.string.ni_title)}: $titleStr"
            isVisible = titleStr.isNotBlank()
        }
        holder.text.apply {
            text = "${context.getString(R.string.ni_content)}: $textStr"
            isVisible = textStr.isNotBlank()
        }
        holder.edit.setOnClickListener(null)
        holder.remove.setOnClickListener(null)

        holder.edit.setOnClickListener {
            val editDialog = MaterialAlertDialogBuilder(
                context,
                com.google.android.material.R.style.MaterialAlertDialog_Material3_Title_Text_CenterStacked
            ).apply {
                setTitle(context.getString(R.string.ni_edit_notify))
                setView(R.layout.layout_ni_dialog)
            }.show()
            val titleView = editDialog.findViewById<TextInputEditText>(R.id.data_title)?.apply {
                setText(titleStr)
            }
            val textView = editDialog.findViewById<TextInputEditText>(R.id.data_text)?.apply {
                setText(textStr)
            }
            editDialog.findViewById<MaterialButton>(R.id.data_save)?.apply {
                setOnClickListener {
                    val title = titleView?.text.toString()
                    val text = textView?.text.toString()
                    if (title.isNotBlank() || text.isNotBlank()) {
                        allDatas[position] = NIInfo(title, text)
                        saveAllData()
                        editDialog.dismiss()
                    }
                }
            }
        }
        holder.remove.setOnClickListener {
            MaterialAlertDialogBuilder(
                context,
                com.google.android.material.R.style.MaterialAlertDialog_Material3_Title_Text_CenterStacked
            ).apply {
                setTitle(context.getString(R.string.dialog_remove_tips))
                setPositiveButton(
                    android.R.string.ok
                ) { _, _ ->
                    allDatas.removeAt(position)
                    saveAllData()
                }
                setNeutralButton(android.R.string.cancel, null)
            }.show()
        }
    }

    override fun getItemCount(): Int {
        return allDatas.size
    }

    fun addData(data: NIInfo) {
        allDatas.add(data)
        saveAllData()
    }

    private fun saveAllData() {
        val datas = ArrayList<String>()
        allDatas.forEach {
            if (it.title.isNotBlank() || it.text.isNotBlank()) datas.add("${it.title}||${it.text}")
        }
        context.modulePrefs.putStringSet(packName, datas.toSet())
        refreshDatas()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun refreshDatas() {
        notifyDataSetChanged()
    }

    class ViewHolder(binding: LayoutNiItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val title = binding.dataTitle
        val text = binding.dataText
        val edit = binding.dataEdit
        val remove = binding.dataRemove
    }
}
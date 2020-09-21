package com.example.gc_last.search

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.gc_last.R
import com.example.gc_last.model.FreshDao
import com.example.gc_last.model.FreshData
import com.example.gc_last.model.SaveItem
import kotlinx.android.synthetic.main.fragment_save.view.*
import kotlinx.android.synthetic.main.list_item_saveitem.view.*

//메인 화면 Adapter
class SearchAdapter(val dao: FreshDao) : PagedListAdapter<SaveItem, SearchAdapter.ItemViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val rootView = LayoutInflater.from(parent.context).inflate(R.layout.list_item_saveitem, parent, false)
        return ItemViewHolder(rootView)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bindItems(getItem(position))
    }

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {        
        fun bindItems(saveItem: SaveItem?) {
            itemView.txt_subway_line_num.text= saveItem?.saveSubwayLineNum!!.replace("0","")
            itemView.txt_subway_name.text = saveItem?.saveTitle
            itemView.txt_subway_days.text = saveItem?.saveSubwayDays
            itemView.txt_subway_direction.text = saveItem?.saveSubwayDirection
            itemView.btn_delete.setOnClickListener {
                saveItem?.id?.let { dao.deleteSaveData(it) }
            }

            itemView.txt_subway_name.setOnClickListener {
                Navigation.findNavController(itemView).navigate(
                    R.id.action_searchFragment_to_saveFragment,
                    Bundle().apply {
                        putLong("SAVE_ID", saveItem!!.id!!)
                    }
                )
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SaveItem>() {
            override fun areItemsTheSame(oldConcert: SaveItem, newConcert: SaveItem): Boolean =
                oldConcert.id == newConcert.id

            override fun areContentsTheSame(oldConcert: SaveItem, newConcert: SaveItem): Boolean =
                oldConcert.id == newConcert.id
        }
    }
}
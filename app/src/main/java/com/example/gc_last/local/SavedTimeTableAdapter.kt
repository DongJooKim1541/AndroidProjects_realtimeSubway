package com.example.gc_last.local

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gc_last.R
import com.example.gc_last.model.FreshData
import kotlinx.android.synthetic.main.fragment_saved_time_table.view.*
import kotlinx.android.synthetic.main.list_item_fresh.view.*
import kotlinx.android.synthetic.main.list_item_timetable.view.*

//역 시간표 Adapter
class SavedTimeTableAdapter : RecyclerView.Adapter<ItemViewHolder>() {

    var freshList: List<FreshData> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val rootView = LayoutInflater.from(parent.context).inflate(R.layout.list_item_timetable, parent, false)
        return ItemViewHolder(rootView)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bindItems(freshList[position])
    }

    override fun getItemCount() = freshList.size
}

class SaveAdpater : PagedListAdapter<FreshData, ItemViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val rootView = LayoutInflater.from(parent.context).inflate(R.layout.list_item_fresh, parent, false)
        return ItemViewHolder(rootView)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bindItems(getItem(position))
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<FreshData>() {
            override fun areItemsTheSame(oldConcert: FreshData, newConcert: FreshData): Boolean =
                oldConcert.id == newConcert.id

            override fun areContentsTheSame(oldConcert: FreshData, newConcert: FreshData): Boolean =
                oldConcert.id == newConcert.id
        }
    }
}

class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    fun bindItems(fresh: FreshData?) {
        fresh?.let {
            itemView.txt_arrive_time.text = fresh.arrivetime
            itemView.txt_subway_endName.text = fresh.subway_end_name+"행"
        }
    }
}
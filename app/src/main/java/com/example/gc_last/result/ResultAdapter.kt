package com.example.gc_last.result

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.gc_last.R
import com.example.gc_last.model.FreshData
import kotlinx.android.synthetic.main.list_item_fresh.view.*

//결과 화면 Adapter
class ResultAdapter() : RecyclerView.Adapter<ItemViewHolder>() {

    var freshList: List<FreshData> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val rootView = LayoutInflater.from(parent.context).inflate(R.layout.list_item_fresh, parent, false)
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
            itemView.txt_timeDistance.text = fresh.timeDistance+" 뒤 도착"
            itemView.txt_subway_end_name.text = fresh.subway_end_name+"행"
        }
    }
}

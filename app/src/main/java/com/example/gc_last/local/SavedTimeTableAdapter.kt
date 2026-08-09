package com.example.gc_last.local

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gc_last.R
import com.example.gc_last.model.FreshData
import kotlinx.android.synthetic.main.list_item_timetable.view.*

/** 시간표 화면 목록 어댑터. `list_item_timetable`을 바인딩한다. */
class SavedTimeTableAdapter : RecyclerView.Adapter<SavedTimeTableAdapter.TimeTableViewHolder>() {

    var freshList: List<FreshData> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeTableViewHolder {
        val rootView = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_timetable, parent, false)
        return TimeTableViewHolder(rootView)
    }

    override fun onBindViewHolder(holder: TimeTableViewHolder, position: Int) =
        holder.bind(freshList[position])

    override fun getItemCount() = freshList.size

    class TimeTableViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(fresh: FreshData) {
            itemView.txt_arrive_time.text = fresh.arrivetime
            itemView.txt_subway_endName.text =
                itemView.context.getString(R.string.bound_for, fresh.subway_end_name)
        }
    }
}

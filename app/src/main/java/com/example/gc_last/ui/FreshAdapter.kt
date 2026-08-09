package com.example.gc_last.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.gc_last.R
import com.example.gc_last.model.FreshData
import kotlinx.android.synthetic.main.list_item_fresh.view.*

/**
 * `list_item_fresh` 바인딩.
 *
 * 리팩토링 이전에는 동일한 어댑터가 `ResultAdapter` / `Save_adapter` / `Save_Adpater`로,
 * 동일한 ViewHolder가 `ItemViewHolder` / `Item_ViewHolder`로 중복되어 있었다.
 */
class FreshViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    fun bind(fresh: FreshData?) {
        fresh ?: return
        itemView.txt_timeDistance.text =
            itemView.context.getString(R.string.arrives_in, fresh.timeDistance)
        itemView.txt_subway_end_name.text =
            itemView.context.getString(R.string.bound_for, fresh.subway_end_name)
    }
}

private fun inflateFreshItem(parent: ViewGroup): FreshViewHolder =
    FreshViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.list_item_fresh, parent, false)
    )

/** 검색 결과 / 새로고침 결과처럼 목록 전체를 한 번에 받는 경우. */
class FreshAdapter : RecyclerView.Adapter<FreshViewHolder>() {

    var freshList: List<FreshData> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = inflateFreshItem(parent)

    override fun onBindViewHolder(holder: FreshViewHolder, position: Int) =
        holder.bind(freshList[position])

    override fun getItemCount() = freshList.size
}

/** 저장된 열차 목록처럼 Room 페이징으로 받는 경우. */
class FreshPagedAdapter : PagedListAdapter<FreshData, FreshViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = inflateFreshItem(parent)

    override fun onBindViewHolder(holder: FreshViewHolder, position: Int) =
        holder.bind(getItem(position))

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<FreshData>() {
            override fun areItemsTheSame(oldItem: FreshData, newItem: FreshData) =
                oldItem.id == newItem.id

            // 이전에는 여기서도 id만 비교해 내용이 바뀌어도 다시 그리지 않았다.
            override fun areContentsTheSame(oldItem: FreshData, newItem: FreshData) =
                oldItem == newItem
        }
    }
}

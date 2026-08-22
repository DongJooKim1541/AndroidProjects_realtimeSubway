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
        bindRemaining(fresh)
        itemView.txt_subway_end_name.text =
            itemView.context.getString(R.string.bound_for, fresh.subway_end_name)
    }

    /** 남은 시간만 갱신한다. 매초 갱신되는 값이라 이 부분만 따로 둔다. */
    fun bindRemaining(fresh: FreshData?) {
        fresh ?: return
        itemView.txt_timeDistance.text =
            itemView.context.getString(R.string.arrives_in, fresh.timeDistance)
    }
}

private fun inflateFreshItem(parent: ViewGroup): FreshViewHolder =
    FreshViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.list_item_fresh, parent, false)
    )

/** 검색 결과 / 새로고침 결과처럼 목록 전체를 한 번에 받는 경우. */
class FreshAdapter : RecyclerView.Adapter<FreshViewHolder>() {

    /**
     * 남은 시간이 매초 갱신되므로, 목록 구성이 그대로일 때는 시간 표시만 바꾼다.
     *
     * 예전처럼 매번 [notifyDataSetChanged] 를 부르면 1초마다 목록 전체를 다시 묶어
     * 화면이 잠시도 idle 상태가 되지 않는다(UI 자동화 도구가 덤프를 못 뜬다).
     */
    var freshList: List<FreshData> = emptyList()
        set(value) {
            val onlyTimeChanged = field.size == value.size &&
                field.indices.all { field[it].sameTrainAs(value[it]) }
            field = value

            if (onlyTimeChanged) {
                notifyItemRangeChanged(0, value.size, PAYLOAD_REMAINING)
            } else {
                notifyDataSetChanged()
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = inflateFreshItem(parent)

    override fun onBindViewHolder(holder: FreshViewHolder, position: Int) =
        holder.bind(freshList[position])

    override fun onBindViewHolder(
        holder: FreshViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.contains(PAYLOAD_REMAINING)) {
            holder.bindRemaining(freshList[position])
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun getItemCount() = freshList.size

    private companion object {
        const val PAYLOAD_REMAINING = "remaining"
    }
}

/** 남은 시간을 뺀 나머지가 같은 편성인지. */
private fun FreshData.sameTrainAs(other: FreshData): Boolean =
    arrivetime == other.arrivetime &&
        station_name == other.station_name &&
        subway_end_name == other.subway_end_name &&
        resultDirection == other.resultDirection

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

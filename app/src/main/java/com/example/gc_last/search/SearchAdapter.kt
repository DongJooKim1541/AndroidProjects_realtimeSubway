package com.example.gc_last.search

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.gc_last.R
import com.example.gc_last.model.SaveItem
import com.example.gc_last.model.SubwayLine
import com.example.gc_last.util.NavKeys
import kotlinx.android.synthetic.main.list_item_saveitem.view.*

/**
 * 저장된 검색 조건 목록 어댑터.
 *
 * 이전에는 어댑터가 [com.example.gc_last.model.FreshDao]를 직접 들고 삭제 쿼리를 메인 스레드에서
 * 실행했다. 지금은 삭제 요청만 [onDelete]로 넘기고 실제 처리는 Fragment/ViewModel이 맡는다.
 */
class SearchAdapter(
    private val onDelete: (Long) -> Unit
) : PagedListAdapter<SaveItem, SearchAdapter.SaveItemViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SaveItemViewHolder {
        val rootView = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_saveitem, parent, false)
        return SaveItemViewHolder(rootView)
    }

    override fun onBindViewHolder(holder: SaveItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SaveItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(saveItem: SaveItem?) {
            saveItem ?: return

            // API가 "02호선"처럼 호선 앞에 0을 붙여 내려준다. 이전 구현은 replace("0","")라
            // "10호선"이 "1호선"이 되는 문제가 있었다.
            val lineLabel = saveItem.saveSubwayLineNum.removePrefix("0")
            itemView.txt_subway_line_num.text = lineLabel
            applyLineColor(itemView, SubwayLine.of(lineLabel))
            itemView.txt_subway_name.text = saveItem.saveTitle
            itemView.txt_subway_days.text = saveItem.saveSubwayDays
            itemView.txt_subway_direction.text = saveItem.saveSubwayDirection

            val saveId = saveItem.id

            itemView.btn_delete.setOnClickListener {
                saveId?.let(onDelete)
            }

            itemView.txt_subway_name.setOnClickListener {
                saveId ?: return@setOnClickListener
                Navigation.findNavController(itemView).navigate(
                    R.id.action_searchFragment_to_saveFragment,
                    Bundle().apply { putLong(NavKeys.SAVE_ID, saveId) }
                )
            }
        }
    }

    /**
     * 호선 배지를 노선색으로 칠한다.
     *
     * 행 그림에는 2호선 초록 원이 박혀 있어 어느 호선을 저장해도 초록으로 보였다.
     * 그림에서 원을 지우고(`save_row_chrome`), 그 자리에 놓인 [R.id.img_line_badge] 를
     * 노선색으로 칠한다.
     */
    private fun applyLineColor(itemView: View, line: SubwayLine?) {
        val color = line?.let { Color.parseColor(it.color) } ?: DEFAULT_BADGE_COLOR
        itemView.img_line_badge.setImageDrawable(
            GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
            }
        )
    }

    companion object {
        /** 노선을 알 수 없을 때 쓰는 배지 색. */
        private val DEFAULT_BADGE_COLOR = Color.parseColor("#8A80A0")

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SaveItem>() {
            override fun areItemsTheSame(oldItem: SaveItem, newItem: SaveItem) =
                oldItem.id == newItem.id

            // 이전에는 여기서도 id만 비교해 내용 변경이 화면에 반영되지 않았다.
            override fun areContentsTheSame(oldItem: SaveItem, newItem: SaveItem) =
                oldItem == newItem
        }
    }
}

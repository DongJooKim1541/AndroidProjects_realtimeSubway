package com.example.gc_last.local

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gc_last.R
import com.example.gc_last.util.NavKeys
import kotlinx.android.synthetic.main.fragment_saved_time_table.*
import kotlinx.android.synthetic.main.fragment_saved_time_table.view.*

/** 저장된 역의 시간표 화면. 첫차/곧 도착/막차/전체를 전환한다. */
class SavedTimeTableFragment : Fragment() {

    private val timeTableViewModel by lazy {
        ViewModelProvider(this).get(SavedTimeTableViewModel::class.java)
    }

    private val timeTableAdapter = SavedTimeTableAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_saved_time_table, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val selectSubway = arguments?.getString(NavKeys.SELECT_SUBWAY)
        val selectDay = arguments?.getString(NavKeys.SELECT_DAY)
        val resultDirection = arguments?.getString(NavKeys.RESULT_DIRECTION)

        if (selectSubway == null || selectDay == null || resultDirection == null) {
            Toast.makeText(requireContext(), R.string.load_failed, Toast.LENGTH_LONG).show()
            return
        }

        view.recycle_timetable.adapter = timeTableAdapter
        view.recycle_timetable.layoutManager = LinearLayoutManager(requireContext())

        // 관찰자는 한 번만 등록한다. 이전에는 버튼 4개가 각자 observe를 호출해
        // 탭할 때마다 관찰자가 쌓였다.
        timeTableViewModel.timeTable.observe(viewLifecycleOwner) { trains ->
            timeTableAdapter.freshList = trains
        }

        timeTableViewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message ?: return@observe
            val text = message.ifBlank { getString(R.string.load_failed) }
            Toast.makeText(requireContext(), text, Toast.LENGTH_LONG).show()
            timeTableViewModel.clearErrorMessage()
        }

        // 필터별로 같은 11줄이 네 번 복사되어 있던 부분.
        val filterButtons = mapOf(
            timetable_startSub to TimeTableFilter.FIRST,
            timetable_realTimeSub to TimeTableFilter.UPCOMING,
            timetable_endSub to TimeTableFilter.LAST,
            timetable_all to TimeTableFilter.ALL
        )

        filterButtons.forEach { (button, filter) ->
            button.setOnClickListener {
                timeTableViewModel.load(selectSubway, selectDay, resultDirection, filter)
            }
        }

        timeTableViewModel.load(selectSubway, selectDay, resultDirection, TimeTableFilter.ALL)
    }
}

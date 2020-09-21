package com.example.gc_last.local

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gc_last.R
import com.example.gc_last.model.FreshData
import kotlinx.android.synthetic.main.fragment_result.view.*
import kotlinx.android.synthetic.main.fragment_saved_time_table.*
import kotlinx.android.synthetic.main.fragment_saved_time_table.view.*
import kotlinx.android.synthetic.main.fragment_saved_time_table.view.timetable_radio_group

//지하철 시간표 화면 구현
class SavedTimeTableFragment : Fragment() {

    val timeTableViewModel by lazy {
        ViewModelProvider(requireActivity()).get(SavedTimeTableViewModel::class.java)
    }

    val timeTableAdpater = SavedTimeTableAdapter()

    lateinit var selectedSubway: String
    lateinit var selectedDay: String
    lateinit var result_direction: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_saved_time_table, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val selectSubway = arguments?.getString("SELECT_SUBWAY")
        val selectDay = arguments?.getString("SELECT_DAY")
        val resultDirection = arguments?.getString("RESULT_DIRECTION")
        if (selectSubway != null && selectDay != null && resultDirection != null) {
            selectedSubway = selectSubway
            selectedDay = selectDay
            result_direction = resultDirection
        }

        if (selectSubway != null && selectDay != null && resultDirection != null) {
            timeTableViewModel.loadDataFromURL(selectSubway, selectDay, resultDirection)
            timeTableViewModel.transferData(selectSubway, selectDay, resultDirection)
        }

        timeTableViewModel.resultList().observe(viewLifecycleOwner, Observer {

            timeTableAdpater.freshList = it
            timeTableAdpater.notifyDataSetChanged()
        })

        view.recycle_timetable.adapter = timeTableAdpater
        view.recycle_timetable.layoutManager = LinearLayoutManager(requireContext())

        //첫차 버튼
        timetable_startSub.setOnClickListener {
            if (selectedSubway != null && selectedDay != null && result_direction != null) {
                timeTableViewModel.loadStartDataFromURL(selectedSubway,selectedDay, result_direction)
                timeTableViewModel.transferData(selectedSubway,selectedDay, result_direction)
            }
            timeTableViewModel.resultList().observe(viewLifecycleOwner, Observer {
                timeTableAdpater.freshList = it
                timeTableAdpater.notifyDataSetChanged()
            })
            view.recycle_timetable.adapter = timeTableAdpater
            view.recycle_timetable.layoutManager = LinearLayoutManager(requireContext())
        }
        //곧 도착 버튼
        timetable_realTimeSub.setOnClickListener {
            if (selectedSubway != null && selectedDay != null && result_direction != null) {
                timeTableViewModel.loadRealTimeDataFromURL(selectedSubway,selectedDay, result_direction)
                timeTableViewModel.transferData(selectedSubway,selectedDay, result_direction)
            }
            timeTableViewModel.resultList().observe(viewLifecycleOwner, Observer {

                timeTableAdpater.freshList = it
                timeTableAdpater.notifyDataSetChanged()
            })
            view.recycle_timetable.adapter = timeTableAdpater
            view.recycle_timetable.layoutManager = LinearLayoutManager(requireContext())
        }
        //막차 버튼
        timetable_endSub.setOnClickListener {
            if (selectedSubway != null && selectedDay != null && result_direction != null) {
                timeTableViewModel.loadEndDataFromURL(selectedSubway,selectedDay, result_direction)
                timeTableViewModel.transferData(selectedSubway,selectedDay, result_direction)
            }
            timeTableViewModel.resultList().observe(viewLifecycleOwner, Observer {
                timeTableAdpater.freshList = it
                timeTableAdpater.notifyDataSetChanged()
            })
            view.recycle_timetable.adapter = timeTableAdpater
            view.recycle_timetable.layoutManager = LinearLayoutManager(requireContext())
        }
        //전체 버튼
        timetable_all.setOnClickListener {
            if (selectedSubway != null && selectedDay != null && result_direction != null) {
                timeTableViewModel.loadDataFromURL(selectedSubway,selectedDay, result_direction)
                timeTableViewModel.transferData(selectedSubway,selectedDay, result_direction)
            }
            timeTableViewModel.resultList().observe(viewLifecycleOwner, Observer {
                timeTableAdpater.freshList = it
                timeTableAdpater.notifyDataSetChanged()
            })
            view.recycle_timetable.adapter = timeTableAdpater
            view.recycle_timetable.layoutManager = LinearLayoutManager(requireContext())
        }
    }
}

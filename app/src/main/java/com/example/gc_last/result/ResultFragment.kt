package com.example.gc_last.result

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gc_last.R
import com.example.gc_last.database.DatabaseModule
import com.example.gc_last.model.Subways
import kotlinx.android.synthetic.main.fragment_result.*
import kotlinx.android.synthetic.main.fragment_result.view.*

//결과 화면
class ResultFragment : Fragment() {

    val resultViewModel by lazy {
        ViewModelProvider(requireActivity()).get(ResultViewModel::class.java)
    }

    val resultAdpater = ResultAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_result, container, false)
    }

    @SuppressLint("RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val selectSubway = arguments?.getString("SELECT_SUBWAY")
        val selectDay = arguments?.getString("SELECT_DAY")
        val resultDirection = arguments?.getString("RESULT_DIRECTION")

        //서버에 검색 요청
        if (selectDay != null && selectSubway != null && resultDirection != null) {

            resultViewModel.loadDataFromURL(selectSubway,selectDay, resultDirection)
            resultViewModel.transferData(selectSubway,selectDay, resultDirection)

             resultViewModel.resultList().observe(viewLifecycleOwner, Observer {
                resultAdpater.freshList = it
                resultAdpater.notifyDataSetChanged()

                view.progress_loader.visibility = View.GONE
                view.floting_save.visibility = View.VISIBLE
            })

            txt_subway_subwayStation.text=Subways.valueOf(selectSubway).holder

            view.recycle_result.adapter = resultAdpater
            view.recycle_result.layoutManager = LinearLayoutManager(requireContext())

            view.floting_save.setOnClickListener {

                if (resultViewModel.resultList().value.isNullOrEmpty()) {
                    Toast.makeText(requireContext(), "저장할 데이터가 없습니다.", Toast.LENGTH_LONG).show()
                } else {

                    resultViewModel.saveResult(
                        requireContext(),
                        "${Subways.valueOf(selectSubway).holder}역","${selectDay}","${if(resultDirection=="1"){"상행"} else "하행"}"
                    )
                    Toast.makeText(requireContext(), "데이터가 저장되었습니다.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}


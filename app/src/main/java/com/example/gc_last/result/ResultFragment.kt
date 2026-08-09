package com.example.gc_last.result

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gc_last.R
import com.example.gc_last.model.Subways
import com.example.gc_last.network.SubwayApi
import com.example.gc_last.ui.FreshAdapter
import com.example.gc_last.util.NavKeys
import kotlinx.android.synthetic.main.fragment_result.*
import kotlinx.android.synthetic.main.fragment_result.view.*

/** 검색 결과 화면. 선택한 역의 다가오는 열차를 보여주고 저장할 수 있다. */
class ResultFragment : Fragment() {

    // 이전에는 requireActivity() 스코프라 화면을 벗어나도 상태가 남았다.
    private val resultViewModel by lazy {
        ViewModelProvider(this).get(ResultViewModel::class.java)
    }

    private val resultAdapter = FreshAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_result, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val selectSubway = arguments?.getString(NavKeys.SELECT_SUBWAY)
        val selectDay = arguments?.getString(NavKeys.SELECT_DAY)
        val resultDirection = arguments?.getString(NavKeys.RESULT_DIRECTION)

        if (selectSubway == null || selectDay == null || resultDirection == null) {
            // 조건 없이 들어온 경우 빈 화면 대신 실패를 알리고 되돌아간다.
            Toast.makeText(requireContext(), R.string.load_failed, Toast.LENGTH_LONG).show()
            return
        }

        view.recycle_result.adapter = resultAdapter
        view.recycle_result.layoutManager = LinearLayoutManager(requireContext())
        txt_subway_subwayStation.text = Subways.valueOf(selectSubway).holder

        resultViewModel.timeTable.observe(viewLifecycleOwner) { trains ->
            resultAdapter.freshList = trains
            view.progress_loader.visibility = View.GONE
            view.floting_save.visibility = View.VISIBLE
        }

        resultViewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message ?: return@observe
            val text = message.ifBlank { getString(R.string.load_failed) }
            Toast.makeText(requireContext(), text, Toast.LENGTH_LONG).show()
            resultViewModel.clearErrorMessage()
        }

        view.floting_save.setOnClickListener {
            onSaveClicked(selectSubway, selectDay, resultDirection)
        }

        resultViewModel.load(selectSubway, selectDay, resultDirection)
    }

    private fun onSaveClicked(selectSubway: String, selectDay: String, resultDirection: String) {
        if (resultViewModel.timeTable.value.isNullOrEmpty()) {
            Toast.makeText(requireContext(), R.string.nothing_to_save, Toast.LENGTH_LONG).show()
            return
        }

        val directionLabel = if (resultDirection == SubwayApi.DIRECTION_UP) {
            getString(R.string.direction_up)
        } else {
            getString(R.string.direction_down)
        }

        resultViewModel.save(
            title = getString(R.string.station_suffix, Subways.valueOf(selectSubway).holder),
            days = selectDay,
            direction = directionLabel
        )
        Toast.makeText(requireContext(), R.string.save_success, Toast.LENGTH_LONG).show()
    }
}

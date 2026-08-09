package com.example.gc_last.local

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gc_last.R
import com.example.gc_last.model.FreshData
import com.example.gc_last.model.Subways
import com.example.gc_last.ui.FreshAdapter
import com.example.gc_last.ui.FreshPagedAdapter
import com.example.gc_last.util.NavKeys
import kotlinx.android.synthetic.main.fragment_save.*
import kotlinx.android.synthetic.main.fragment_save.view.*

/** 저장된 역 상세 화면. 저장 당시 결과, 시간표, 주변 버스/출구 정보로 이동한다. */
class SaveFragment : Fragment() {

    private val saveViewModel by lazy {
        ViewModelProvider(this).get(SaveViewModel::class.java)
    }

    private val savedAdapter = FreshPagedAdapter()
    private val refreshedAdapter = FreshAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_save, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val saveId = arguments?.getLong(NavKeys.SAVE_ID) ?: return

        view.list_save.adapter = savedAdapter
        view.list_save.layoutManager = LinearLayoutManager(requireContext())

        saveViewModel.savedTrains(saveId).observe(viewLifecycleOwner) { savedAdapter.submitList(it) }

        // 관찰자는 onViewCreated에서 한 번만 등록한다.
        // 이전에는 새로고침 버튼을 누를 때마다 새 관찰자가 쌓였다.
        saveViewModel.refreshed.observe(viewLifecycleOwner) { trains ->
            refreshedAdapter.freshList = trains
            view.list_save.adapter = refreshedAdapter
        }

        saveViewModel.savedCondition.observe(viewLifecycleOwner) { condition ->
            bindCondition(view, condition)
        }

        saveViewModel.loadSavedCondition(saveId)
    }

    private fun bindCondition(view: View, condition: FreshData?) {
        if (condition == null) {
            Toast.makeText(requireContext(), R.string.no_saved_condition, Toast.LENGTH_LONG).show()
            return
        }

        val subway = condition.selectSubway ?: return
        val day = condition.selectDay ?: return
        val direction = condition.resultDirection ?: return
        val stationName = Subways.valueOf(subway).holder

        txt_subway_subwayStation.text = condition.station_name

        txt_subway_timetable.setOnClickListener {
            findNavController().navigate(
                R.id.action_saveFragment_to_savedTimeTableFragment,
                Bundle().apply {
                    putString(NavKeys.SELECT_SUBWAY, subway)
                    putString(NavKeys.SELECT_DAY, day)
                    putString(NavKeys.RESULT_DIRECTION, direction)
                }
            )
        }

        txt_subway_bus.setOnClickListener { openExternal(busSearchUrl(stationName)) }
        txt_subway_exit_info.setOnClickListener { openExternal(exitSearchUrl(stationName)) }

        image_reset.setOnClickListener {
            view.list_save.layoutManager = LinearLayoutManager(requireContext())
            saveViewModel.refresh(subway, day, direction)
        }
    }

    private fun openExternal(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun busSearchUrl(stationName: String) =
        "$NAVER_MAP_BASE/bus/search.nhn?query=$stationName역+버스&tab=BUS_ROUTE&busType=&queryRank=1"

    private fun exitSearchUrl(stationName: String) =
        "$NAVER_MAP_BASE/search2/search.nhn?query=$stationName역%20출구&sm=shistory&style=v5"

    companion object {
        private const val NAVER_MAP_BASE = "https://m.map.naver.com"
    }
}

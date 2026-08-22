package com.example.gc_last.search

import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gc_last.R
import com.example.gc_last.model.Station
import com.example.gc_last.model.StationCatalog
import com.example.gc_last.network.SubwayApi
import com.example.gc_last.util.NavKeys
import kotlinx.android.synthetic.main.fragment_search.*
import kotlinx.android.synthetic.main.fragment_search.view.*

/**
 * 메인 화면. 노선도에서 역을 고르고, 요일·방향을 라디오 버튼으로 골라 결과 화면으로 넘어간다.
 *
 * 예전에는 역과 요일을 각각 콤보 박스(AlertDialog 목록)로 골랐고 2호선 20개 역만 있었다.
 * 지금은 전체 노선도를 화면에 두고 그 위에서 고른다.
 */
class SearchFragment : Fragment() {

    private var selectedStation: Station? = null
    private var selectedDay: String? = null

    private val searchViewModel by lazy {
        ViewModelProvider(this).get(SearchViewModel::class.java)
    }

    private val searchAdapter by lazy {
        SearchAdapter(onDelete = { saveId -> searchViewModel.delete(saveId) })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_search, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpMap(view)
        setUpDayRadio(view)

        view.list_search.adapter = searchAdapter
        view.list_search.layoutManager = LinearLayoutManager(requireContext())
        searchViewModel.savedItems.observe(viewLifecycleOwner) { searchAdapter.submitList(it) }

        setUpTabs(view)

        view.btn_search.setOnClickListener { onSearchClicked(view) }

        // requireActivity() 대신 viewLifecycleOwner에 묶어 화면이 사라질 때 콜백이 해제되도록 한다.
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = showExitDialog()
            }
        )

        updateSearchButtonState()
    }

    private fun setUpMap(view: View) {
        val map = view.map_network

        view.btn_zoom_in.setOnClickListener { map.zoomIn() }
        view.btn_zoom_out.setOnClickListener { map.zoomOut() }

        map.onStationClick = { station ->
            if (station.timeTableSupported) {
                selectedStation = station
                map.selected = station
                // 고른 역이 가운데 오도록 확대해 준다. 축소 상태에서는 이름이 안 보인다.
                map.focusOn(station)
                val transfers = StationCatalog.transfersOf(station)
                text_type.text = if (transfers.isEmpty()) {
                    getString(R.string.selected_station, station.line.label, station.name)
                } else {
                    getString(
                        R.string.selected_station_with_transfer,
                        station.line.label,
                        station.name,
                        transfers.joinToString(", ") { it.label }
                    )
                }
            } else {
                // 노선도에는 그리되, 시간표 API가 데이터를 주지 않는 노선임을 알린다.
                Toast.makeText(
                    requireContext(),
                    getString(R.string.timetable_unsupported_line, station.line.label),
                    Toast.LENGTH_SHORT
                ).show()
            }
            updateSearchButtonState()
        }

    }

    private fun setUpDayRadio(view: View) {
        view.radio_day.setOnCheckedChangeListener { group, checkedId ->
            val tag = group.findViewById<RadioButton>(checkedId)?.tag?.toString() ?: return@setOnCheckedChangeListener
            selectedDay = tag
            txt_weekday.text = getString(R.string.selected_day, tag)
            updateSearchButtonState()
        }
    }

    /**
     * 노선도와 저장 목록을 탭으로 나눈다.
     *
     * 저장 목록을 노선도 아래에 함께 두면 노선도가 그만큼 가려진다. 지도는 넓어야 쓸 수
     * 있으므로 화면을 나눠 번갈아 보여준다.
     */
    private fun setUpTabs(view: View) {
        fun select(showMap: Boolean) {
            view.pane_map.visibility = if (showMap) View.VISIBLE else View.GONE
            view.list_search.visibility = if (showMap) View.GONE else View.VISIBLE

            val active = ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
            val inactive = ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
            view.tab_map.setBackgroundColor(if (showMap) active else inactive)
            view.tab_saved.setBackgroundColor(if (showMap) inactive else active)
            view.tab_map.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (showMap) android.R.color.white else android.R.color.black
                )
            )
            view.tab_saved.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (showMap) android.R.color.black else android.R.color.white
                )
            )
        }

        view.tab_map.setOnClickListener { select(true) }
        view.tab_saved.setOnClickListener { select(false) }
        select(true)
    }

    private fun onSearchClicked(view: View) {
        val station = selectedStation
        val day = selectedDay

        if (station == null || day == null) {
            Toast.makeText(requireContext(), R.string.search_input_required, Toast.LENGTH_LONG).show()
            return
        }

        findNavController().navigate(
            R.id.action_searchFragment_to_resultFragment,
            Bundle().apply {
                // 역 코드를 넘긴다. 예전에는 Subways 상수 이름을 넘겼고 2호선만 가능했다.
                putString(NavKeys.SELECT_SUBWAY, station.code)
                putString(NavKeys.SELECT_DAY, day)
                putString(NavKeys.RESULT_DIRECTION, view.selectedDirection())
            }
        )
    }

    /** 아무것도 선택되지 않았으면 상행으로 본다. 이전에는 이 경우 NPE가 났다. */
    private fun View.selectedDirection(): String {
        val checkedId = radio_layout.checkedRadioButtonId
        if (checkedId == View.NO_ID) return SubwayApi.DIRECTION_UP
        return findViewById<RadioButton>(checkedId)?.tag?.toString() ?: SubwayApi.DIRECTION_UP
    }

    private fun showExitDialog() {
        AlertDialog.Builder(
            ContextThemeWrapper(requireContext(), R.style.Theme_AppCompat_Light_Dialog)
        )
            .setMessage(R.string.exit_confirm_message)
            .setPositiveButton(R.string.exit) { _, _ -> activity?.finish() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * 이전 구현에는 조건을 만족할 때 초록색으로 바꾸는 분기만 있어서,
     * 검색 후 선택이 초기화되어도 버튼이 계속 초록색으로 남았다.
     */
    private fun updateSearchButtonState() {
        val ready = selectedStation != null && selectedDay != null
        val background = if (ready) android.R.color.holo_green_dark else android.R.color.darker_gray
        val text = if (ready) android.R.color.white else android.R.color.black

        btn_search.setBackgroundColor(ContextCompat.getColor(requireContext(), background))
        btn_search.setTextColor(ContextCompat.getColor(requireContext(), text))
    }
}

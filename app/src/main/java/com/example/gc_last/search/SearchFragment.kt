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
import com.example.gc_last.model.DayOfWeek
import com.example.gc_last.model.Subways
import com.example.gc_last.network.SubwayApi
import com.example.gc_last.util.NavKeys
import kotlinx.android.synthetic.main.fragment_search.*
import kotlinx.android.synthetic.main.fragment_search.view.*

/** 메인 화면. 역/요일/방향을 고르고 결과 화면으로 넘어간다. */
class SearchFragment : Fragment() {

    private var selectedSubway: String? = null
    private var selectedDay: String? = null

    private val searchViewModel by lazy {
        ViewModelProvider(this).get(SearchViewModel::class.java)
    }

    private val searchAdapter by lazy {
        SearchAdapter(onDelete = { saveId -> searchViewModel.delete(saveId) })
    }

    private val stationDialog by lazy {
        selectionDialog(
            titleRes = R.string.select_station_title,
            labels = Subways.values().map { it.holder }
        ) { index ->
            with(Subways.values()[index]) {
                selectedSubway = name
                text_type.text = holder
            }
        }
    }

    private val dayDialog by lazy {
        selectionDialog(
            titleRes = R.string.select_day_title,
            labels = DayOfWeek.values().map { it.holder }
        ) { index ->
            with(DayOfWeek.values()[index]) {
                selectedDay = name
                txt_weekday.text = holder
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_search, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        layout_type.setOnClickListener { stationDialog.show() }
        layout_date.setOnClickListener { dayDialog.show() }

        view.list_search.adapter = searchAdapter
        view.list_search.layoutManager = LinearLayoutManager(requireContext())

        searchViewModel.savedItems.observe(viewLifecycleOwner) { searchAdapter.submitList(it) }

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

    private fun onSearchClicked(view: View) {
        val subway = selectedSubway
        val day = selectedDay

        if (subway == null || day == null) {
            Toast.makeText(requireContext(), R.string.search_input_required, Toast.LENGTH_LONG).show()
            return
        }

        findNavController().navigate(
            R.id.action_searchFragment_to_resultFragment,
            Bundle().apply {
                putString(NavKeys.SELECT_SUBWAY, subway)
                putString(NavKeys.SELECT_DAY, day)
                putString(NavKeys.RESULT_DIRECTION, view.selectedDirection())
            }
        )

        selectedSubway = null
        selectedDay = null
        updateSearchButtonState()
    }

    /** 아무것도 선택되지 않았으면 상행으로 본다. 이전에는 이 경우 NPE가 났다. */
    private fun View.selectedDirection(): String {
        val checkedId = radio_layout.checkedRadioButtonId
        if (checkedId == View.NO_ID) return SubwayApi.DIRECTION_UP
        return findViewById<RadioButton>(checkedId)?.tag?.toString() ?: SubwayApi.DIRECTION_UP
    }

    private fun selectionDialog(
        titleRes: Int,
        labels: List<String>,
        onSelected: (index: Int) -> Unit
    ): AlertDialog = AlertDialog.Builder(requireContext())
        .setTitle(titleRes)
        .setItems(labels.toTypedArray()) { _, index ->
            onSelected(index)
            updateSearchButtonState()
        }
        .setNegativeButton(R.string.cancel, null)
        .create()

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
        val ready = selectedSubway != null && selectedDay != null
        val background = if (ready) android.R.color.holo_green_dark else android.R.color.darker_gray
        val text = if (ready) android.R.color.white else android.R.color.black

        btn_search.setBackgroundColor(ContextCompat.getColor(requireContext(), background))
        btn_search.setTextColor(ContextCompat.getColor(requireContext(), text))
    }
}

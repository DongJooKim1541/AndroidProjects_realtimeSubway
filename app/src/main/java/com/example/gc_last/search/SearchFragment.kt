package com.example.gc_last.search

import android.os.Bundle
import android.util.Log
import android.view.ContextThemeWrapper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LiveData
import androidx.navigation.fragment.findNavController
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gc_last.R
import com.example.gc_last.database.DatabaseModule
import com.example.gc_last.model.DayOfWeek
import com.example.gc_last.model.Subways
import com.example.gc_last.model.SaveItem
import kotlinx.android.synthetic.main.fragment_search.*
import kotlinx.android.synthetic.main.fragment_search.view.*
import kotlinx.android.synthetic.main.fragment_search.view.btn_search

//메인 화면
class SearchFragment : Fragment(){

    var selectedSubway: String? = null
    var selectedDay: String? = null

    val database by lazy {
        DatabaseModule.getDatabase(requireContext())
    }

    val searchAdapter by lazy { SearchAdapter(database.freshDao()) }
    //지하철역 선택 dialog
    val alertDialog1 by lazy {

        val builder = AlertDialog.Builder(requireContext())

        builder.setTitle("지하철역을 선택하세요.")

        builder.setItems(Subways.values().map { it.holder }.toTypedArray()) { dialog, index ->
            with(Subways.values()[index]) {
                selectedSubway = this.name
                text_type.text = this.holder
            }
            checkCondition()
        }

        builder.setNegativeButton("취소", null)

        builder.create()
    }
    //요일 선택 dialog
    val alertDialog2 by lazy {

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("날짜를 선택하세요.")

        builder.setItems(DayOfWeek.values().map { it.holder }.toTypedArray()) { dialog, index ->
            with(DayOfWeek.values()[index]) {
                selectedDay = this.name

                txt_weekday.text = this.holder
            }
            checkCondition()
        }

        builder.setNegativeButton("취소", null)

        builder.create()
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        layout_type.setOnClickListener { alertDialog1.show() }
        layout_date.setOnClickListener { alertDialog2.show() }

        view.list_search.adapter = searchAdapter
        view.list_search.layoutManager = LinearLayoutManager(requireContext())

        val pageLiveData: LiveData<PagedList<SaveItem>> = LivePagedListBuilder(database.freshDao().loadSaveItems(), 100).build()

        pageLiveData.observe(viewLifecycleOwner, androidx.lifecycle.Observer {

            searchAdapter.submitList(it)
        })

        view.btn_search.setOnClickListener {

            if (selectedSubway == null || selectedDay == null) {

                Toast.makeText(requireContext(), "지하철역과 날짜를 입력해주세요.", Toast.LENGTH_LONG).show()
            } else {

                findNavController().navigate(
                    R.id.action_searchFragment_to_resultFragment,
                    Bundle().apply {
                        putString("SELECT_SUBWAY", selectedSubway)
                        putString("SELECT_DAY", selectedDay)
                        putString("RESULT_DIRECTION", view.findViewById<RadioButton>(view.radio_layout.checkedRadioButtonId).tag.toString())
                    })
                selectedSubway = null
                selectedDay = null
            }
        }

        activity?.onBackPressedDispatcher?.addCallback(requireActivity(), object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val builder= AlertDialog.Builder(ContextThemeWrapper(requireContext(), R.style.Theme_AppCompat_Light_Dialog))
                
                builder.setMessage("앱을 종료하시겠습니까?")
                builder.setPositiveButton("종료") { _, _ ->
                   activity?.finish()
                }
                builder.setNegativeButton("취소") { _, _ ->
                }
                builder.show()
            }
        })
    }

    private fun changeInputTextBydate() {
        checkCondition()

        selectedDay?.let { txt_weekday.text = it }
    }

    private fun checkCondition() {
        if (selectedSubway != null && selectedDay != null) {
            btn_search.setBackgroundColor(resources.getColor(android.R.color.holo_green_dark))
            btn_search.setTextColor(resources.getColor(android.R.color.white))
        }
    }
}


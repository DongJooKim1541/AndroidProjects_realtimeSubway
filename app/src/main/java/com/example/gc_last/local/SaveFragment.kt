package com.example.gc_last.local

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gc_last.R
import com.example.gc_last.database.DatabaseModule
import com.example.gc_last.model.FreshData
import com.example.gc_last.model.Subways
import com.example.gc_last.result.SaveViewModel
import com.example.gc_last.result.Save_Adpater
import com.example.gc_last.result.Save_adapter
import kotlinx.android.synthetic.main.fragment_save.*
import kotlinx.android.synthetic.main.fragment_save.view.*

//역 정보 저장소 화면 구현
class SaveFragment : Fragment() {

    val database by lazy {
        DatabaseModule.getDatabase(requireContext())
    }

    val saveViewModel by lazy {
        ViewModelProvider(requireActivity()).get(SaveViewModel::class.java)
    }

    val saveAdapter by lazy { Save_Adpater() }
    val save_Adpater = Save_adapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_save, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.list_save.adapter = saveAdapter
        view.list_save.layoutManager = LinearLayoutManager(requireContext())

        arguments?.getLong("SAVE_ID")?.let { saveId ->

            val selectSubway=database.freshDao().loadFreshSubwayData(saveId = (saveId*2)-1)
            val selectDay=database.freshDao().loadFreshDayData(saveId = (saveId*2)-1)
            val resultDirection=database.freshDao().loadFreshDirectionData(saveId = (saveId*2)-1)
            val pageLiveData: LiveData<PagedList<FreshData>> = LivePagedListBuilder(database.freshDao().loadFreshData(saveId = saveId),20).build()

            pageLiveData.observe(viewLifecycleOwner, androidx.lifecycle.Observer {

                saveAdapter.submitList(it)
                txt_subway_subwayStation.text=database.freshDao().loadFreshStationNameData(saveId = (saveId*2)-1)
            })
            //시간표
            txt_subway_timetable.setOnClickListener {

                arguments?.getLong("SAVE_ID")?.let { saveId ->

                    findNavController().navigate(
                        R.id.action_saveFragment_to_savedTimeTableFragment,
                        Bundle().apply {
                            putString("SELECT_SUBWAY", selectSubway)
                            putString("SELECT_DAY", selectDay)
                            putString("RESULT_DIRECTION", resultDirection)
                        })
                }
            }
            //버스 정보
            txt_subway_bus.setOnClickListener {
                val intent= Intent(Intent.ACTION_VIEW, Uri.parse(
                    "https://m.map.naver.com/bus/search.nhn?query=${ Subways.valueOf(selectSubway.toString()).holder}역+버스&tab=BUS_ROUTE&busType=&queryRank=1"))
                Log.d("SaveFragment", "https://m.map.naver.com/search2/search.nhn?query=${ Subways.valueOf(selectSubway.toString()).holder}역%20출구&sm=shistory&style=v5")
                startActivity(intent)
            }
            //지하철 출구 정보
            txt_subway_exit_info.setOnClickListener {
                val intent= Intent(Intent.ACTION_VIEW, Uri.parse(
                    "https://m.map.naver.com/search2/search.nhn?query=${ Subways.valueOf(selectSubway.toString()).holder}역%20출구&sm=shistory&style=v5"))
                Log.d("SaveFragment", "https://m.map.naver.com/search2/search.nhn?query=${ Subways.valueOf(selectSubway.toString()).holder}역%20출구&sm=shistory&style=v5")
                startActivity(intent)
            }

            image_reset.setOnClickListener {

                arguments?.getLong("SAVE_ID")?.let { saveId ->

                    if(selectSubway!=null && selectDay!=null && resultDirection!=null){

                        saveViewModel.loadDataFromURL(selectSubway,selectDay, resultDirection)
                        saveViewModel.transferData(selectSubway,selectDay, resultDirection)
                        saveViewModel.resultList().observe(viewLifecycleOwner, Observer {

                            save_Adpater.freshList = it
                            save_Adpater.notifyDataSetChanged()
                        })

                        view.list_save.adapter = save_Adpater
                        view.list_save.layoutManager = LinearLayoutManager(requireContext())
                    }
                    else{
                        Toast.makeText(requireContext(), "해당되는 데이터가 없습니다!", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}


package com.example.gc_last.result

import android.content.Context
import android.util.Log
import android.widget.Adapter
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gc_last.database.DatabaseModule
import com.example.gc_last.model.*
import com.example.gc_last.network.NetworkModule
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import javax.xml.parsers.DocumentBuilderFactory

//역 정보 저장 화면 ViewModel
class SaveViewModel: ViewModel() {

    lateinit var selectSubway:String
    lateinit var selectDay:String
    lateinit var resultDirection:String

    private val resultList: MutableLiveData<List<FreshData>> = MutableLiveData()
    fun resultList(): LiveData<List<FreshData>> = resultList

    val errorHandler = CoroutineExceptionHandler { _, exception ->
        Log.e("error", exception.message)
    }

    fun transferData(selectStationCode: String, selectWeekTag: String, selectInOutTag: String){
        selectSubway=selectStationCode
        selectDay=selectWeekTag
        resultDirection=selectInOutTag
    }

    fun loadDataFromURL(
        selectStationCode: String,
        selectWeekTag: String,
        selectInOutTag: String
    ) {
        val request = NetworkModule.makeHttprequest_subwayTime(

            NetworkModule.makeHttpUrl_subwayTime(
                station_code = Subways.valueOf(selectStationCode).scode,
                week_tag = DayOfWeek.valueOf(selectWeekTag).weekcode,
                inout_tag = selectInOutTag
            )
        )

        viewModelScope.launch(Dispatchers.IO + errorHandler) {

            val xml : Document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(request.url.toString())
            xml.documentElement.normalize()

            val list:NodeList=xml.getElementsByTagName("row")

            val lists = mutableListOf<FreshData>()
            lateinit var timeDistance:String
            for(i in 0..list.length-1){
                var n: Node =list.item(i)

                if(n.getNodeType()==Node.ELEMENT_NODE){
                    val elem=n as Element

                    val map=mutableMapOf<String,String>()

                    for(j in 0..elem.attributes.length - 1){
                        map.putIfAbsent(elem.attributes.item(j).nodeName, elem.attributes.item(j).nodeValue)
                    }
                    val line_num=elem.getElementsByTagName("LINE_NUM").item(0).textContent.toString()
                    val station_name=elem.getElementsByTagName("STATION_NM").item(0).textContent.toString()
                    val arrivetime=elem.getElementsByTagName("ARRIVETIME").item(0).textContent.toString()
                    val subway_end_name=elem.getElementsByTagName("SUBWAYENAME").item(0).textContent.toString()
                    val select_Subway=selectSubway
                    val select_Day=selectDay
                    val result_Direction=resultDirection

                    val timeCal=TimeCalculate(arrivetime)

                    if(timeCal.diff.hours==0 && timeCal.diff.minutes>=0 && timeCal.diff.seconds>=0){
                        if(lists.size<2){
                            timeDistance="${timeCal.diff.minutes}분 ${timeCal.diff.seconds}초"
                            val fresh=FreshData( null,null,line_num,station_name,arrivetime,subway_end_name,timeDistance,select_Subway,select_Day,result_Direction)
                            lists.add(fresh)
                        }
                        else if(timeCal.diff.hours>0){
                            if(lists.size<2){
                                timeDistance="${timeCal.diff.hours}시간 ${timeCal.diff.minutes}분 ${timeCal.diff.seconds}초"
                                val fresh=FreshData( null,null,line_num,station_name,arrivetime,subway_end_name,timeDistance,select_Subway,select_Day,result_Direction)
                                lists.add(fresh)
                            }
                        }
                    }
                }
            }
            Log.d("XML","List의 크기: ${lists.size}")
            resultList.postValue(lists)
        }
    }
}





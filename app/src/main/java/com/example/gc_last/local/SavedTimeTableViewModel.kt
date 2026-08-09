package com.example.gc_last.local

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gc_last.data.SubwayRepository
import com.example.gc_last.data.upcoming
import com.example.gc_last.model.FreshData
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** 시간표 화면에서 고를 수 있는 조회 범위. */
enum class TimeTableFilter {
    /** 전체 시간표 */
    ALL,

    /** 첫차 */
    FIRST,

    /** 막차 */
    LAST,

    /** 곧 도착하는 열차 1편 */
    UPCOMING
}

/**
 * 저장된 역의 시간표 ViewModel.
 *
 * 이전에는 필터별로 `loadDataFromURL` / `loadStartDataFromURL` / `loadEndDataFromURL` /
 * `loadRealTimeDataFromURL` 네 함수가 같은 파싱 로직을 그대로 복제하고 있었다(250줄).
 * 조회는 [SubwayRepository]가 담당하고, 여기서는 [TimeTableFilter]로 결과만 추린다.
 */
class SavedTimeTableViewModel : ViewModel() {

    private val _timeTable = MutableLiveData<List<FreshData>>()
    val timeTable: LiveData<List<FreshData>> = _timeTable

    private val errorHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, throwable.message ?: throwable.javaClass.simpleName, throwable)
        _timeTable.postValue(emptyList())
    }

    fun load(subwayName: String, dayName: String, direction: String, filter: TimeTableFilter) {
        viewModelScope.launch(Dispatchers.IO + errorHandler) {
            val all = SubwayRepository.loadTimeTable(subwayName, dayName, direction)
            _timeTable.postValue(all.applyFilter(filter))
        }
    }

    /** 빈 응답에서도 안전하도록 first/last는 [listOfNotNull]로 감싼다. */
    private fun List<FreshData>.applyFilter(filter: TimeTableFilter): List<FreshData> = when (filter) {
        TimeTableFilter.ALL -> this
        TimeTableFilter.FIRST -> listOfNotNull(firstOrNull())
        TimeTableFilter.LAST -> listOfNotNull(lastOrNull())
        TimeTableFilter.UPCOMING -> upcoming(UPCOMING_LIMIT)
    }

    companion object {
        private const val TAG = "SavedTimeTableViewModel"

        /** "곧 도착"은 가장 가까운 1편만 보여준다. */
        private const val UPCOMING_LIMIT = 1
    }
}

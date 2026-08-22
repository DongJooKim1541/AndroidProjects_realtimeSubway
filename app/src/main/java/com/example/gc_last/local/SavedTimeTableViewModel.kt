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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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

    private val _errorMessage = MutableLiveData<String?>()

    /** 조회 실패 사유. 성공 시 null. */
    val errorMessage: LiveData<String?> = _errorMessage

    private val errorHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, throwable.message ?: throwable.javaClass.simpleName, throwable)
        _errorMessage.postValue(throwable.message)
        _timeTable.postValue(emptyList())
    }

    /** 조회해 둔 전체 시간표. "곧 도착"은 여기서 매초 다시 계산한다. */
    private var timetable: List<FreshData> = emptyList()

    private var ticker: Job? = null

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun load(subwayName: String, dayName: String, direction: String, filter: TimeTableFilter) {
        viewModelScope.launch(Dispatchers.IO + errorHandler) {
            timetable = SubwayRepository.loadTimeTable(subwayName, dayName, direction)
            _timeTable.postValue(timetable.applyFilter(filter))
            restartTicker(filter)
        }
    }

    /**
     * "곧 도착"만 매초 다시 계산한다.
     *
     * 전체/첫차/막차는 시간표 그대로라 값이 바뀌지 않으므로, 갱신하면 239행 목록을
     * 1초마다 헛되이 다시 그리게 된다.
     */
    private fun restartTicker(filter: TimeTableFilter) {
        ticker?.cancel()
        if (filter != TimeTableFilter.UPCOMING) return

        ticker = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(TICK_INTERVAL_MS)
                _timeTable.postValue(timetable.applyFilter(TimeTableFilter.UPCOMING))
            }
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

        /** 남은 시간 갱신 주기. */
        private const val TICK_INTERVAL_MS = 1_000L
    }
}

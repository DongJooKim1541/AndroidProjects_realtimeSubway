package com.example.gc_last.result

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.gc_last.data.SubwayRepository
import com.example.gc_last.data.upcoming
import com.example.gc_last.database.DatabaseModule
import com.example.gc_last.model.FreshData
import com.example.gc_last.model.SaveItem
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** 검색 결과 화면 ViewModel. 선택한 역의 다가오는 열차 [UPCOMING_LIMIT]편을 보여준다. */
class ResultViewModel(application: Application) : AndroidViewModel(application) {

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

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun load(subwayName: String, dayName: String, direction: String) {
        viewModelScope.launch(Dispatchers.IO + errorHandler) {
            _timeTable.postValue(
                SubwayRepository.loadTimeTable(subwayName, dayName, direction).upcoming(UPCOMING_LIMIT)
            )
        }
    }

    /** 현재 조회 결과를 저장소에 담는다. 저장 전에 결과가 비어 있지 않은지 호출부에서 확인한다. */
    fun save(title: String, days: String, direction: String) {
        val results = _timeTable.value.orEmpty()
        if (results.isEmpty()) return

        val dao = DatabaseModule.getDatabase(getApplication()).freshDao()
        viewModelScope.launch(Dispatchers.IO + errorHandler) {
            val saveId = dao.insertSave(
                SaveItem(
                    id = null,
                    saveTitle = title,
                    saveSubwayDirection = direction,
                    saveSubwayDays = days,
                    saveSubwayLineNum = results.first().line_num,
                    saveSubwayStationName = results.first().station_name
                )
            )
            dao.insertFresh(results.map { it.copy(saveId = saveId) })
        }
    }

    companion object {
        private const val TAG = "ResultViewModel"

        /** 결과 화면에 노출할 다가오는 열차 편수. */
        private const val UPCOMING_LIMIT = 2
    }
}

package com.example.gc_last.local

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.example.gc_last.data.SubwayRepository
import com.example.gc_last.data.upcoming
import com.example.gc_last.database.DatabaseModule
import com.example.gc_last.model.FreshData
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 저장된 역 상세 화면 ViewModel.
 *
 * 이전에는 Fragment가 DAO를 메인 스레드에서 직접 호출했다. 조회는 모두 이쪽으로 옮겼다.
 */
class SaveViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = DatabaseModule.getDatabase(application).freshDao()

    private val _savedCondition = MutableLiveData<FreshData?>()

    /** 저장 당시의 역/요일/방향. 시간표·버스·출구 화면으로 넘길 조건이다. */
    val savedCondition: LiveData<FreshData?> = _savedCondition

    private val _refreshed = MutableLiveData<List<FreshData>>()

    /** 새로고침으로 다시 조회한 실시간 결과. */
    val refreshed: LiveData<List<FreshData>> = _refreshed

    private val errorHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, throwable.message ?: throwable.javaClass.simpleName, throwable)
        _refreshed.postValue(emptyList())
    }

    fun savedTrains(saveId: Long): LiveData<PagedList<FreshData>> =
        LivePagedListBuilder(dao.loadFreshData(saveId), PAGE_SIZE).build()

    fun loadSavedCondition(saveId: Long) {
        viewModelScope.launch(Dispatchers.IO + errorHandler) {
            _savedCondition.postValue(dao.loadSavedCondition(saveId))
        }
    }

    fun refresh(subwayName: String, dayName: String, direction: String) {
        viewModelScope.launch(Dispatchers.IO + errorHandler) {
            _refreshed.postValue(
                SubwayRepository.loadTimeTable(subwayName, dayName, direction).upcoming(UPCOMING_LIMIT)
            )
        }
    }

    companion object {
        private const val TAG = "SaveViewModel"
        private const val PAGE_SIZE = 20
        private const val UPCOMING_LIMIT = 2
    }
}

package com.example.gc_last.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.example.gc_last.database.DatabaseModule
import com.example.gc_last.model.SaveItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** 검색 화면에 표시할 저장 목록과 삭제를 담당한다. */
class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = DatabaseModule.getDatabase(application).freshDao()

    val savedItems: LiveData<PagedList<SaveItem>> =
        LivePagedListBuilder(dao.loadSaveItems(), PAGE_SIZE).build()

    fun delete(saveId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteSaveData(saveId)
        }
    }

    companion object {
        private const val PAGE_SIZE = 100
    }
}

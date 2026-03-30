package com.hussein.mawaqit.presentation.quran.list_screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hussein.mawaqit.data.db.QuranDatabaseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SurahListViewModel(
    private val quranDatabaseRepository: QuranDatabaseRepository,
) : ViewModel() {

    val allSurahs = quranDatabaseRepository.getAllSurahs().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val bookmarks = quranDatabaseRepository.getAllBookmarks().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun deleteBookmark(surahNumber: Int, ayahNumber: Int) {
        viewModelScope.launch {
            quranDatabaseRepository.removeBookmark(surahNumber, ayahNumber)
        }
    }

}
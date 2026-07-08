package com.hussein.mawaqit.presentation.quran.list_screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hussein.mawaqit.data.db.repo.QuranDatabaseRepository
import com.hussein.mawaqit.infrastructure.settings.QuranReaderPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

data class LastRead(
    val surahNumber: Int,
    val ayahNumber: Int,
    val surahName: String
)

class SurahListViewModel(
    private val quranDatabaseRepository: QuranDatabaseRepository,
    private val quranReaderPreferences: QuranReaderPreferences
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    val surahs = quranDatabaseRepository.getAllSurahs()
        .onEach { _isLoading.value = false }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val lastReadPosition: StateFlow<LastRead?> = combine(
        surahs,
        quranReaderPreferences.lastReadSurah,
        quranReaderPreferences.lastReadAyah
    ) { surahList, lastSurah, lastAyah ->
        if (lastSurah != null && lastAyah != null) {
            val surah = surahList.find { it.number == lastSurah }
            if (surah != null) {
                LastRead(lastSurah, lastAyah, surah.nameArabic)
            } else null
        } else null
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

}

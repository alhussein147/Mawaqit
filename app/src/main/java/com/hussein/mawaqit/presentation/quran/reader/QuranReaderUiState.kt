package com.hussein.mawaqit.presentation.quran.reader

import com.hussein.mawaqit.data.db.models.SurahDetail


sealed interface QuranReaderUiState {
    data object Idle : QuranReaderUiState
    data object Loading : QuranReaderUiState
    data class Success(val surah: SurahDetail) : QuranReaderUiState
    data class Error(val message: String) : QuranReaderUiState
}
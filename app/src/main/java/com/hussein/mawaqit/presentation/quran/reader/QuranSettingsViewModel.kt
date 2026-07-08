package com.hussein.mawaqit.presentation.quran.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hussein.mawaqit.infrastructure.settings.QuranReaderPreferences
import com.hussein.mawaqit.infrastructure.settings.QuranTextAlignment
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class QuranSettingsViewModel(
    private val quranReaderPreferences: QuranReaderPreferences
) : ViewModel() {

    val textAlignment: StateFlow<QuranTextAlignment> = quranReaderPreferences.quranTextAlignment
        .stateIn(viewModelScope, SharingStarted.Eagerly, QuranTextAlignment.Center)

    val fontSize: StateFlow<Float> = quranReaderPreferences.fontSizeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 18f)

    fun setTextAlignment(alignment: QuranTextAlignment) {
        viewModelScope.launch {
            quranReaderPreferences.setTextAlignmentSize(alignment)
        }
    }

    fun setFontSize(size: Float) {
        viewModelScope.launch {
            quranReaderPreferences.setFontSize(size)
        }
    }
}

package com.hussein.mawaqit.presentation.quran.tafsir

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hussein.mawaqit.data.db.repo.TafsirRepository
import com.hussein.mawaqit.domain.models.AyahWithTafsir
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface QuranTafsirUiState {
    data object Loading : QuranTafsirUiState
    data class Success(val ayahs: List<AyahWithTafsir>) : QuranTafsirUiState
    data class Error(val message: String) : QuranTafsirUiState
}

class QuranReaderWithTafsirViewModel(
    private val tafsirRepository: TafsirRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuranTafsirUiState>(QuranTafsirUiState.Loading)
    val uiState: StateFlow<QuranTafsirUiState> = _uiState.asStateFlow()

    fun loadTafsir(surahNumber: Int) {
        viewModelScope.launch {
            _uiState.value = QuranTafsirUiState.Loading
            try {
                val data = tafsirRepository.fetchSurahWithTafsir(surahNumber)
                _uiState.value = QuranTafsirUiState.Success(data)
            } catch (e: Exception) {
                _uiState.value = QuranTafsirUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

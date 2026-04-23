package com.hussein.mawaqit.presentation.azkar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hussein.mawaqit.data.azkar.AzkarCategory
import com.hussein.mawaqit.data.azkar.AzkarRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AzkarViewModel(private val azkarRepository: AzkarRepository) : ViewModel() {


    // Titles loaded once from files
    private val _categoryTitles = MutableStateFlow<List<String>>(emptyList())
    val categoryTitles: StateFlow<List<String>> = _categoryTitles.asStateFlow()

    // Holds the currently loaded category (Idle until user picks one)
    private val _listState = MutableStateFlow<AzkarListState>(AzkarListState.Idle)
    val listState: StateFlow<AzkarListState> = _listState.asStateFlow()

    private var loadedIndex = -1


    init {
        viewModelScope.launch {
            _categoryTitles.value = runCatching {
                azkarRepository.loadMetadata().map { it.title }
            }.getOrDefault(emptyList())
        }
    }

    fun selectCategory(index: Int) {
        if (loadedIndex == index && _listState.value is AzkarListState.Success) _listState.update {
            AzkarListState.Error(
                "Unknown Error"
            )
        }
        viewModelScope.launch {
            _listState.update { AzkarListState.Loading }
            try {
                _listState.update {
                    AzkarListState.Success(azkarRepository.loadCategory(index))
                        .also { loadedIndex = index }
                }
            } catch (e: Exception) {
                _listState.update {
                    AzkarListState.Error("Failed to load azkar: ${e.message}")
                }
            }
        }
    }
}


sealed interface AzkarListState {
    data object Idle : AzkarListState
    data object Loading : AzkarListState
    data class Success(val category: AzkarCategory) : AzkarListState
    data class Error(val message: String) : AzkarListState
}

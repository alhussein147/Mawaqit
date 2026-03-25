package com.hussein.mawaqit.presentation.azkar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hussein.mawaqit.MawaqitApp
import com.hussein.mawaqit.data.azkar.AzkarCategory
import com.hussein.mawaqit.data.azkar.AzkarRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
        if (loadedIndex == index && _listState.value is AzkarListState.Success) return
        viewModelScope.launch {
            _listState.value = AzkarListState.Loading
            _listState.value = try {
                AzkarListState.Success(azkarRepository.loadCategory(index)).also { loadedIndex = index }
            } catch (e: Exception) {
                AzkarListState.Error("Failed to load azkar: ${e.message}")
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

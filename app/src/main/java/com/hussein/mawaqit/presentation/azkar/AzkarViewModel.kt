package com.hussein.mawaqit.presentation.azkar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hussein.mawaqit.AppContainer
import com.hussein.mawaqit.MyApp
import com.hussein.mawaqit.data.azkar.AzkarCategory
import com.hussein.mawaqit.data.azkar.AzkarRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AzkarViewModel(private val azkarRepository: AzkarRepository) : ViewModel() {


    // Static — no I/O needed
    val categoryTitles: List<String> = azkarRepository.categoryTitles

    // Holds the currently loaded category (Idle until user picks one)
    private val _listState = MutableStateFlow<AzkarListState>(AzkarListState.Idle)
    val listState: StateFlow<AzkarListState> = _listState.asStateFlow()

    fun selectCategory(index: Int) {
        viewModelScope.launch {
            _listState.value = AzkarListState.Loading
            _listState.value = try {
                AzkarListState.Success(azkarRepository.loadCategory(index))
            } catch (e: Exception) {
                AzkarListState.Error("Failed to load azkar: ${e.message}")
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as MyApp)
                AzkarViewModel(azkarRepository = AzkarRepository(application))
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

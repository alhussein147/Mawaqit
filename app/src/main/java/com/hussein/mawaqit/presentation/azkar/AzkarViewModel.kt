package com.hussein.mawaqit.presentation.azkar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hussein.mawaqit.data.azkar.AzkarCategory
import com.hussein.mawaqit.data.azkar.AzkarRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AzkarViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = AzkarRepository(application)

    // Static — no I/O needed
    val categoryTitles: List<String> = repo.categoryTitles

    // Holds the currently loaded category (Idle until user picks one)
    private val _listState = MutableStateFlow<AzkarListState>(AzkarListState.Idle)
    val listState: StateFlow<AzkarListState> = _listState.asStateFlow()

    fun selectCategory(index: Int) {
        viewModelScope.launch {
            _listState.value = AzkarListState.Loading
            _listState.value = try {
                AzkarListState.Success(repo.loadCategory(index))
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

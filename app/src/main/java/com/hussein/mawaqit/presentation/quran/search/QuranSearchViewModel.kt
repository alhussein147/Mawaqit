package com.hussein.mawaqit.presentation.quran.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hussein.mawaqit.data.db.AyahEntity
import com.hussein.mawaqit.data.db.QuranDatabaseRepository
import com.hussein.mawaqit.data.db.SurahEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class SearchResults(
    val surahs: List<SurahEntity> = emptyList(),
    val ayahs: List<AyahEntity> = emptyList()
) {
    val isEmpty get() = surahs.isEmpty() && ayahs.isEmpty()
}

sealed interface SearchState {
    data object Idle : SearchState
    data object Loading : SearchState
    data class Results(val data: SearchResults) : SearchState
    data object Empty : SearchState
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class QuranSearchViewModel(
    private val repo: QuranDatabaseRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _state = MutableStateFlow<SearchState>(SearchState.Idle)
    val state: StateFlow<SearchState> = _state.asStateFlow()

    init {
        _query
            .debounce(300)
            .distinctUntilChanged()
            .flatMapLatest { q ->
                if (q.isBlank()) {
                    _state.value = SearchState.Idle
                    return@flatMapLatest flowOf(null)
                }
                _state.value = SearchState.Loading
                combine(
                    repo.searchSurahs(q),
                    repo.searchAyahs(q)
                ) { surahs, ayahs -> SearchResults(surahs, ayahs) }
            }
            .onEach { results ->
                if (results == null) return@onEach
                _state.value = if (results.isEmpty) SearchState.Empty
                else SearchState.Results(results)
            }
            .launchIn(viewModelScope)
    }

    fun onQueryChanged(q: String) {
        _query.value = q
    }

    fun clearQuery() {
        _query.value = ""
    }
}

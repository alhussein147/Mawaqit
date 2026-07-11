package com.hussein.mawaqit.presentation.quran.search

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hussein.mawaqit.data.db.entities.SurahEntity
import com.hussein.mawaqit.data.db.relations.AyahWithSurah
import com.hussein.mawaqit.data.db.repo.QuranDatabaseRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Duration.Companion.milliseconds

data class SearchResults(
    val surahs: List<SurahEntity> = emptyList(),
    val ayahs: List<AyahWithSurah> = emptyList()
) {
    val isEmpty get() = surahs.isEmpty() && ayahs.isEmpty()
}

sealed interface SearchState {
    data object Idle : SearchState
    data object Loading : SearchState
    data class Results(val data: SearchResults) : SearchState
    data object Empty : SearchState
}

@Stable
data class QuranSearchUiState(
    val query: String = "",
    val searchState: SearchState = SearchState.Idle
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class QuranSearchViewModel(
    private val repo: QuranDatabaseRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val searchFlow = _query
        .debounce(300.milliseconds)
        .distinctUntilChanged()
        .flatMapLatest { q ->
            if (q.isBlank()) {
                flowOf(SearchState.Idle)
            } else {
                combine(
                    repo.searchSurahs(q),
                    repo.searchAyahs(q)
                ) { surahs, ayahs ->
                    val results = SearchResults(surahs, ayahs)
                    if (results.isEmpty) SearchState.Empty
                    else SearchState.Results(results)
                }.onStart { emit(SearchState.Loading) }
            }
        }

    val uiState: StateFlow<QuranSearchUiState> = combine(_query, searchFlow) { q, state ->
        QuranSearchUiState(q, state)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = QuranSearchUiState()
    )

    fun onQueryChanged(q: String) {
        _query.value = q
    }

    fun clearQuery() {
        _query.value = ""
    }
}

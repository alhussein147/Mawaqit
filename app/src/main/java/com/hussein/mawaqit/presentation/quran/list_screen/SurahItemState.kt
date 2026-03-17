package com.hussein.mawaqit.presentation.quran.list_screen

import java.util.UUID

sealed interface SurahItemState {
    data object NotDownloaded : SurahItemState
    data class Downloading(val progress: Float, val workId: UUID) : SurahItemState
    data object Downloaded : SurahItemState
    data object Playing : SurahItemState
    data object Paused : SurahItemState
}
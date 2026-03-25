package com.hussein.mawaqit.presentation.quran.reader

sealed interface RecitationState {
    data object Idle : RecitationState
    data object Buffering : RecitationState
    data object Playing : RecitationState
}

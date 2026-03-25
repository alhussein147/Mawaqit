package com.hussein.mawaqit.presentation.quran.reader

sealed interface AyahRecitationState {
    data object Idle : AyahRecitationState
    data object Buffering : AyahRecitationState
    data object Playing : AyahRecitationState
}

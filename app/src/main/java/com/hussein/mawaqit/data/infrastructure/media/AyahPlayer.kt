package com.hussein.mawaqit.data.infrastructure.media

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.hussein.mawaqit.presentation.quran.reader.AyahRecitationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AyahPlayer(val context: Context) {

    private val _state = MutableStateFlow<AyahRecitationState>(AyahRecitationState.Idle)
    val state: StateFlow<AyahRecitationState> = _state.asStateFlow()

    private val _playingAyah = MutableStateFlow<Int?>(null)
    val playingAyah: StateFlow<Int?> = _playingAyah.asStateFlow()

    private val player = ExoPlayer.Builder(context.applicationContext).build().apply {
        addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> _state.value = AyahRecitationState.Buffering
                    Player.STATE_READY -> _state.value =
                        if (isPlaying) AyahRecitationState.Playing else AyahRecitationState.Idle

                    Player.STATE_ENDED -> stop()
                    else -> Unit
                }
            }
        })
    }

    fun play(url: String, ayahNumber: Int) {
        _playingAyah.value = ayahNumber
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.play()
        _state.value = AyahRecitationState.Buffering
    }

    fun stop() {
        player.stop()
        _playingAyah.value = null
        _state.value = AyahRecitationState.Idle
    }

    fun release() = player.release()

}
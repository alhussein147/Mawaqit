package com.hussein.mawaqit

import android.app.KeyguardManager
import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.Player

class AzanActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_IS_FAJR = "extra_is_fajr"
        const val NOTIFICATION_ID = 9001
    }

    private var exoPlayer: ExoPlayer? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION") window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        // Dismiss keyguard so activity shows on top of lock screen
        val keyguard = getSystemService(KeyguardManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            keyguard.requestDismissKeyguard(this, null)
        }

        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "Prayer"
        val isFajr = intent.getBooleanExtra(EXTRA_IS_FAJR, false)

        audioManager = getSystemService(AudioManager::class.java)

        if (requestAudioFocus()) {
            playAzan(isFajr)
        }

        setContent {
            MaterialTheme {
                AzanScreen(
                    prayerName = prayerName, onDismiss = { dismiss() })
            }
        }
    }

    // ── Audio ─────────────────────────────────────────────────────────────────

    private fun requestAudioFocus(): Boolean {
        val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()

        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attrs).setOnAudioFocusChangeListener { change ->
                if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                    dismiss()
                }
            }.build()

        return audioManager?.requestAudioFocus(focusRequest!!) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun playAzan(isFajr: Boolean) {
        val rawRes = R.raw.azan2
        val uri = "android.resource://$packageName/$rawRes".toUri()
        exoPlayer = ExoPlayer.Builder(this).setAudioAttributes(
                androidx.media3.common.AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_ALARM).build(),/* handleAudioFocus= */
                false // we manage focus manually
            ).build().apply {
                setMediaItem(MediaItem.fromUri(uri))
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_ENDED) dismiss()
                    }
                })
                prepare()
                play()
            }
    }

    private fun releasePlayer() {
        exoPlayer?.apply { stop(); release() }
        exoPlayer = null
        focusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    // ── Dismiss ───────────────────────────────────────────────────────────────

    private fun dismiss() {
        releasePlayer()
        // Cancel the full screen notification
        getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
        finish()
    }

    override fun onDestroy() {
        releasePlayer()
        super.onDestroy()
    }
}

// ---------------------------------------------------------------------------
// Full screen UI
// ---------------------------------------------------------------------------

@Composable
private fun AzanScreen(
    prayerName: String, onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Prayer Time",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = prayerName,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(60.dp))

            Button(
                onClick = onDismiss, modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Dismiss", style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
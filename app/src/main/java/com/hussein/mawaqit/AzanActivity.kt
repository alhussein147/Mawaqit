package com.hussein.mawaqit
import androidx.media3.common.audio.AudioManagerCompat.requestAudioFocus
import android.app.KeyguardManager
import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import com.hussein.mawaqit.ui.theme.MawaqitTheme

@UnstableApi
class AzanActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_IS_FAJR = "extra_is_fajr"
        const val NOTIFICATION_ID = 9001
    }
    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)


        // Dismiss keyguard so activity shows on top of lock screen
        val keyguard = getSystemService(KeyguardManager::class.java)
        keyguard.requestDismissKeyguard(this, null)

        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "Prayer"
        val isFajr = intent.getBooleanExtra(EXTRA_IS_FAJR, false)

        audioManager = getSystemService(AudioManager::class.java)
        requestAudioFocus(audioManager,focusRequest)
        if () {
            playAzan(isFajr)
        }

        enableEdgeToEdge()
        setContent {
            MawaqitTheme {
                AzanScreen(
                    prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "Prayer",
                    onDismiss = {
                        finish()
                    }
                )
            }
        }
    }

    private fun playAzan(isFajr: Boolean, attributes: AudioAttributes) {
        // Use the isFajr flag to select the correct resource
        val rawRes = if (isFajr) R.raw.azan2 else R.raw.azan2

        mediaPlayer = MediaPlayer.create(this, rawRes).apply {
            setAudioAttributes(attributes)
            setOnCompletionListener { dismiss() }
            setOnErrorListener { _, _, _ ->
                dismiss()
                true
            }
            start()
        }
    }
    private fun releasePlayer() {
        mediaPlayer?.apply { if (isPlaying) stop(); release() }
        mediaPlayer = null
        focusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        focusRequest = null
    }
    private fun dismiss() {
        releasePlayer()
        // Cancel the full screen notification
        getSystemService(NotificationManager::class.java)
            .cancel(NOTIFICATION_ID)
        finish()
    }
    override fun onDestroy() {
        releasePlayer()
        super.onDestroy()
    }
}


@Composable
private fun AzanScreen(
    prayerName: String,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
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
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Dismiss",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
package com.hussein.mawaqit.data.infrastructure.services


import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import com.hussein.mawaqit.R

/**
 * Foreground service that plays the azan audio when a prayer time is reached
 * and the screen is off.
 *
 * Stops when:
 *  - Audio finishes naturally
 *  - User dismisses the notification (ACTION_STOP broadcast)
 *  - User presses any media/volume button (via MediaButton broadcast)
 *  - Audio focus is lost (call received, another app takes focus)
 *
 */
class AzanPlayerService : Service() {

    companion object {
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_IS_FAJR = "extra_is_fajr"

        const val ACTION_STOP = "com.hussein.islamic.AZAN_STOP"

        const val NOTIFICATION_ID = 9001
        const val AZAN_CHANNEL_ID = "azan_player"
        const val AZAN_CHANNEL_NAME = "Azan Player"

        fun buildIntent(context: Context, prayerName: String, isFajr: Boolean) =
            Intent(context, AzanPlayerService::class.java).apply {
                putExtra(EXTRA_PRAYER_NAME, prayerName)
                putExtra(EXTRA_IS_FAJR, isFajr)
            }
    }

    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null

    // Receives ACTION_STOP (notification dismiss) and media button presses
    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_STOP) stopSelf()
        }
    }
    private var mediaSession: MediaSessionCompat? = null


    // ---------------------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------------------

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AudioManager::class.java)
        registerStopReceiver()
        setupMediaSession()
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val prayerName = intent?.getStringExtra(EXTRA_PRAYER_NAME) ?: "Prayer"
        val isFajr = intent?.getBooleanExtra(EXTRA_IS_FAJR, false) ?: false

        startForeground(NOTIFICATION_ID, buildNotification(prayerName))

        if (requestAudioFocus()) {
            playAzan(isFajr)
        } else {
            // Another app holds focus (e.g. phone call) — notification is already
            // showing, just don't play audio
            stopSelf()
        }

        return START_NOT_STICKY // don't restart if killed
    }

    override fun onDestroy() {
        releasePlayer()
        abandonAudioFocus()
        unregisterStopReceiver()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null



    private fun requestAudioFocus(): Boolean {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)        // respects DND alarm override
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener { focusChange ->
                // Stop if focus is lost (incoming call, another alarm, etc.)
                if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                    focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                    stopSelf()
                }
            }
            .build()

        return audioManager?.requestAudioFocus(focusRequest!!) ==
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonAudioFocus() {
        focusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    // ---------------------------------------------------------------------------
    // Playback
    // ---------------------------------------------------------------------------


    private fun playAzan(isFajr: Boolean) {
        val rawRes = if (isFajr) R.raw.azan14 else R.raw.azan2

        mediaPlayer = MediaPlayer.create(this, rawRes)?.apply {
            setOnCompletionListener { stopSelf() }
            setOnErrorListener { _, _, _ -> stopSelf(); true }
            start()
        }

        if (mediaPlayer == null) stopSelf() // res/raw file missing — fail gracefully
    }

    private fun releasePlayer() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
    }


    private fun buildNotification(prayerName: String) =
        NotificationCompat.Builder(this, AZAN_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_placeholder)
            .setContentTitle("🕌 $prayerName")
            .setContentText("It's time for $prayerName prayer")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(false)       // prevents swipe-dismiss while azan plays
            .setDeleteIntent(buildStopIntent())  // fires ACTION_STOP when dismissed
            .build()

    private fun buildStopIntent(): PendingIntent {
        val intent = Intent(ACTION_STOP).setPackage(packageName)
        return PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(this, "AzanPlayer").apply {
            // Volume key (and any media key) down → stop the azan
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onMediaButtonEvent(intent: Intent): Boolean {
                    val keyEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                    }
                    if (keyEvent?.action == KeyEvent.ACTION_DOWN) {
                        stopSelf()
                        return true
                    }
                    return false
                }
            })
            isActive = true
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerStopReceiver() {
        val filter = IntentFilter().apply {
            addAction(ACTION_STOP)
            addAction(Intent.ACTION_MEDIA_BUTTON)
            priority = IntentFilter.SYSTEM_HIGH_PRIORITY
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stopReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(stopReceiver, filter)
        }
    }

    private fun unregisterStopReceiver() {
        try {
            unregisterReceiver(stopReceiver)
        } catch (_: IllegalArgumentException) {
        }
    }
}
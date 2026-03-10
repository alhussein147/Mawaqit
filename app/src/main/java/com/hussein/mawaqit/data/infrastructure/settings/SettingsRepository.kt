package com.hussein.mawaqit.data.infrastructure.settings


import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.batoulapps.adhan2.CalculationMethod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Single source of truth for all user preferences.
 *
 * Uses Jetpack DataStore (Preferences) instead of SharedPreferences —
 * coroutine-safe, Flow-based, no main-thread I/O.
 */
class SettingsRepository(private val context: Context) {

    // ---------------------------------------------------------------------------
    // Keys
    // ---------------------------------------------------------------------------

    companion object {
        // Per-prayer notification toggles
        val NOTIFY_FAJR = booleanPreferencesKey("notify_fajr")
        val NOTIFY_DHUHR = booleanPreferencesKey("notify_dhuhr")
        val NOTIFY_ASR = booleanPreferencesKey("notify_asr")
        val NOTIFY_MAGHRIB = booleanPreferencesKey("notify_maghrib")
        val NOTIFY_ISHA = booleanPreferencesKey("notify_isha")

        val CALCULATION_METHOD = stringPreferencesKey("calculation_method")
        val NOTIFICATION_SOUND = stringPreferencesKey("notification_sound")
        val APP_THEME = stringPreferencesKey("app_theme")
        val APP_COLOR_SCHEME = stringPreferencesKey("app_color_scheme")
        val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")

    }


    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            prayerNotifications = PrayerNotificationSettings(
                fajr = prefs[NOTIFY_FAJR] ?: true,
                dhuhr = prefs[NOTIFY_DHUHR] ?: true,
                asr = prefs[NOTIFY_ASR] ?: true,
                maghrib = prefs[NOTIFY_MAGHRIB] ?: true,
                isha = prefs[NOTIFY_ISHA] ?: true
            ),
            calculationMethod = CalculationMethod.valueOf(
                prefs[CALCULATION_METHOD] ?: CalculationMethod.MUSLIM_WORLD_LEAGUE.name
            ),
            notificationSound = NotificationSound.valueOf(
                prefs[NOTIFICATION_SOUND] ?: NotificationSound.ADHAN.name
            ),
            appTheme = AppTheme.valueOf(
                prefs[APP_THEME] ?: AppTheme.SYSTEM.name
            ),
            appColorScheme = AppColorScheme.valueOf(
                prefs[APP_COLOR_SCHEME] ?: AppColorScheme.DYNAMIC.name
            )
        )
    }


    suspend fun setPrayerNotificationEnabled(prayer: String, enabled: Boolean) {
        val key = prayerNotificationKey(prayer) ?: return
        context.dataStore.edit { it[key] = enabled }
    }

    suspend fun setCalculationMethod(method: CalculationMethod) {
        context.dataStore.edit { it[CALCULATION_METHOD] = method.name }
    }

    suspend fun setNotificationSound(sound: NotificationSound) {
        context.dataStore.edit { it[NOTIFICATION_SOUND] = sound.name }
    }

    suspend fun setAppTheme(theme: AppTheme) {
        context.dataStore.edit { it[APP_THEME] = theme.name }
    }

    suspend fun setAppColorScheme(scheme: AppColorScheme) {
        context.dataStore.edit { it[APP_COLOR_SCHEME] = scheme.name }
    }

    private fun prayerNotificationKey(prayer: String): Preferences.Key<Boolean>? = when (prayer) {
        "Fajr" -> NOTIFY_FAJR
        "Dhuhr" -> NOTIFY_DHUHR
        "Asr" -> NOTIFY_ASR
        "Maghrib" -> NOTIFY_MAGHRIB
        "Isha" -> NOTIFY_ISHA
        else -> null
    }

    /** Convenience: returns whether a specific prayer's notification is enabled. */
    fun isNotificationEnabled(prayerName: String, settings: AppSettings): Boolean =
        when (prayerName) {
            "Fajr" -> settings.prayerNotifications.fajr
            "Dhuhr" -> settings.prayerNotifications.dhuhr
            "Asr" -> settings.prayerNotifications.asr
            "Maghrib" -> settings.prayerNotifications.maghrib
            "Isha" -> settings.prayerNotifications.isha
            else -> true
        }


    val isOnboardingDone: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_DONE] ?: false
    }

    suspend fun isOnboardingDone(): Boolean =
        isOnboardingDone.firstOrNull() ?: false

    suspend fun setOnboardingDone() {
        context.dataStore.edit { it[KEY_ONBOARDING_DONE] = true }
    }
}


data class AppSettings(
    val prayerNotifications: PrayerNotificationSettings,
    val calculationMethod: CalculationMethod,
    val notificationSound: NotificationSound,
    val appTheme: AppTheme,
    val appColorScheme: AppColorScheme
)

data class PrayerNotificationSettings(
    val fajr: Boolean,
    val dhuhr: Boolean,
    val asr: Boolean,
    val maghrib: Boolean,
    val isha: Boolean
)

enum class NotificationSound(val displayName: String) {
    ADHAN("Adhan"),
    NOTIFICATION("Notification"),
    NONE("None - Don't Show Notifications")
}

enum class AppTheme(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System Default")
}

enum class AppColorScheme(val displayName: String) {
    DYNAMIC("Dynamic (Material You)"),
    CUSTOM("Mawaqit Theme")
}
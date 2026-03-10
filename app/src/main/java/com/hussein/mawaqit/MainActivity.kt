package com.hussein.mawaqit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hussein.mawaqit.data.infrastructure.settings.SettingsRepository
import com.hussein.mawaqit.presentation.AppNavigation
import com.hussein.mawaqit.ui.theme.IslamicTheme

class MainActivity : ComponentActivity() {

    lateinit var settingsRepo: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        settingsRepo = (this.application as MyApp).appContainer.settingsRepository
        setContent {
            val settings by settingsRepo.settingsFlow
                .collectAsStateWithLifecycle(initialValue = null)

            settings?.let { s ->
                IslamicTheme(
                    appTheme = s.appTheme,
                    appColorScheme = s.appColorScheme
                ) {
                    AppNavigation(
                        settingsRepository = settingsRepo
                    )
                }
            }
        }
    }
}


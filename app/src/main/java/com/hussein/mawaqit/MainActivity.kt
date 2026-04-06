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
import com.hussein.mawaqit.ui.theme.MawaqitTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val settingsRepository: SettingsRepository by inject<SettingsRepository>()
        setContent {
            val settings by settingsRepository.settingsFlow
                .collectAsStateWithLifecycle(initialValue = null)

            settings?.let { s ->
                MawaqitTheme(
                    appTheme = s.appTheme,
                    appColorScheme = s.appColorScheme
                ) {
                    AppNavigation(
                        settingsRepository = settingsRepository
                    )
                }
            }
        }
    }
}


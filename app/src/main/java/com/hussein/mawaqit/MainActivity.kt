package com.hussein.mawaqit

import com.hussein.mawaqit.presentation.AppNavigation
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.hussein.mawaqit.data.infrastructure.settings.SettingsRepository
import com.hussein.mawaqit.ui.theme.IslamicTheme

class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {

        installSplashScreen()

        super.onCreate(savedInstanceState)
        val settingsRepo = SettingsRepository(this)

        enableEdgeToEdge()
        setContent {
            IslamicTheme {
                AppNavigation(
                    settingsRepository = settingsRepo
                )
            }
        }
    }
}


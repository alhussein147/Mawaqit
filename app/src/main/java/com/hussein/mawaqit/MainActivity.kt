package com.hussein.mawaqit

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.os.Bundle
import android.view.View
import android.view.animation.AnticipateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hussein.mawaqit.infrastructure.settings.SettingsRepository
import com.hussein.mawaqit.presentation.navigation.AppNavigation
import com.hussein.mawaqit.ui.theme.MawaqitTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.2f)
            val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.2f)
            val alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0f)

            val animator = ObjectAnimator.ofPropertyValuesHolder(
                splashScreenView.iconView,
                scaleX,
                scaleY,
                alpha
            )
            animator.interpolator = AnticipateInterpolator()
            animator.duration = 500L
            animator.doOnEnd {
                splashScreenView.remove()
            }
            animator.start()
        }

        var isReady = false
        splashScreen.setKeepOnScreenCondition { !isReady }

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val settingsRepository: SettingsRepository by inject<SettingsRepository>()
        setContent {
            val settings by settingsRepository.settingsFlow
                .collectAsStateWithLifecycle(initialValue = null)

            settings?.let { settings ->
                isReady = true
                MawaqitTheme(
                    appTheme = settings.appTheme,
                    appColorScheme = settings.appColorScheme
                ) {
                    AppNavigation(
                        settingsRepository = settingsRepository
                    )
                }
            }
        }
    }
}


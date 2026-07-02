package com.hussein.mawaqit.presentation.onboarding.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.batoulapps.adhan2.CalculationMethod
import com.hussein.mawaqit.R
import com.hussein.mawaqit.infrastructure.settings.AppSettings
import com.hussein.mawaqit.infrastructure.settings.AppTheme
import com.hussein.mawaqit.infrastructure.settings.NotificationSound
import com.hussein.mawaqit.presentation.shared.SettingPickerRow

@Composable
fun OptionsPage(
    settings: AppSettings?,
    onCalculationMethodChanged: (CalculationMethod) -> Unit,
    onNotificationSoundChanged: (NotificationSound) -> Unit,
    onAppThemeChanged: (AppTheme) -> Unit
) {
    if (settings == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.onboarding_options_title),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Personalize your experience before we begin.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(Modifier.height(32.dp))

        SettingPickerRow(
            label = "Calculation Method",
            currentValue = settings.calculationMethod.displayName(),
            options = CalculationMethod.entries.filter { it != CalculationMethod.OTHER }.map { it.displayName() },
            onOptionSelected = { name ->
                val method = CalculationMethod.entries.first { it.displayName() == name }
                onCalculationMethodChanged(method)
            }
        )

        Spacer(Modifier.height(16.dp))

        SettingPickerRow(
            label = "Notification Style",
            currentValue = settings.notificationSound.displayName,
            options = NotificationSound.entries.map { it.displayName },
            onOptionSelected = { name ->
                onNotificationSoundChanged(NotificationSound.entries.first { it.displayName == name })
            }
        )

        Spacer(Modifier.height(16.dp))

        SettingPickerRow(
            label = "App Theme",
            currentValue = settings.appTheme.displayName,
            options = AppTheme.entries.map { it.displayName },
            onOptionSelected = { name ->
                onAppThemeChanged(AppTheme.entries.first { it.displayName == name })
            }
        )
        
        Spacer(Modifier.height(32.dp))
    }
}

private fun CalculationMethod.displayName(): String = when (this) {
    CalculationMethod.MUSLIM_WORLD_LEAGUE -> "Muslim World League"
    CalculationMethod.EGYPTIAN -> "Egyptian"
    CalculationMethod.KARACHI -> "Karachi"
    CalculationMethod.UMM_AL_QURA -> "Umm Al-Qura"
    CalculationMethod.DUBAI -> "Dubai"
    CalculationMethod.MOON_SIGHTING_COMMITTEE -> "Moon Sighting Committee"
    CalculationMethod.NORTH_AMERICA -> "North America (ISNA)"
    CalculationMethod.KUWAIT -> "Kuwait"
    CalculationMethod.QATAR -> "Qatar"
    CalculationMethod.SINGAPORE -> "Singapore"
    CalculationMethod.TURKEY -> "Turkey"
    CalculationMethod.OTHER -> ""
}

package com.hussein.mawaqit.presentation.onboarding.components.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.hussein.mawaqit.ui.theme.MawaqitTheme

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
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.onboarding_options_title),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.personalize_your_experience_before_we_begin),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )


        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            SettingPickerRow(
                shape = MawaqitTheme.appShapes.large,
                label = stringResource(R.string.calculation_method),
                currentValue = settings.calculationMethod.displayName(),
                options = CalculationMethod.entries.filter { it != CalculationMethod.OTHER }
                    .map { it.displayName() },
                onOptionSelected = { name ->
                    val method = CalculationMethod.entries.first { it.displayName() == name }
                    onCalculationMethodChanged(method)
                }
            )

            SettingPickerRow(
                shape = MawaqitTheme.appShapes.large,
                label = stringResource(R.string.notification_style),
                currentValue = settings.notificationSound.displayName,
                options = NotificationSound.entries.map { it.displayName },
                onOptionSelected = { name ->
                    onNotificationSoundChanged(NotificationSound.entries.first { it.displayName == name })
                }
            )

            SettingPickerRow(
                shape = MawaqitTheme.appShapes.large,
                label = "App Theme",
                currentValue = settings.appTheme.displayName,
                options = AppTheme.entries.map { it.displayName },
                onOptionSelected = { name ->
                    onAppThemeChanged(AppTheme.entries.first { it.displayName == name })
                }
            )
        }



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

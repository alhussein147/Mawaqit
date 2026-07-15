package com.hussein.mawaqit.presentation.onboarding.components.pages

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hussein.mawaqit.R
import com.hussein.mawaqit.presentation.onboarding.components.PermissionState
import com.hussein.mawaqit.ui.theme.MawaqitTheme

@Composable
fun PermissionsPage(
    locationState: PermissionState,
    notificationState: PermissionState,
    exactAlarmState: PermissionState,
    batteryOptimizationState: PermissionState,
    onLocationClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onExactAlarmClick: () -> Unit,
    onBatteryClick: () -> Unit,
    isLoadingLocation: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.onboarding_permissions_title),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.onboarding_permissions_page_header),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(Modifier.height(32.dp))

        PermissionItem(
            title = stringResource(R.string.onboarding_location_title),
            description = stringResource(R.string.onboarding_location_description),
            iconRes = R.drawable.ic_location,
            state = locationState,
            onClick = onLocationClick,
            isLoading = isLoadingLocation
        )

        PermissionItem(
            title = stringResource(R.string.onboarding_notification_title),
            description = stringResource(R.string.onboarding_notification_description),
            iconRes = R.drawable.ic_bell,
            state = notificationState,
            onClick = onNotificationClick
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PermissionItem(
                title = stringResource(R.string.onboarding_exact_alarm_title),
                description = stringResource(R.string.onboarding_exact_alarm_description),
                iconRes = R.drawable.ic_alarm,
                state = exactAlarmState,
                onClick = onExactAlarmClick
            )
        }

        PermissionItem(
            title = stringResource(R.string.onboarding_battery_optimization_title),
            description = stringResource(R.string.onboarding_battery_optimization_description),
            iconRes = R.drawable.ic_battery,
            state = batteryOptimizationState,
            onClick = onBatteryClick
        )
        
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PermissionItem(
    title: String,
    description: String,
    iconRes: Int,
    state: PermissionState,
    onClick: () -> Unit,
    isLoading: Boolean = false
) {
    val isGranted = state.isGranted
    val isDenied = state.isDenied

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(MawaqitTheme.appShapes.medium)
            .clickable(enabled = !isGranted && !isLoading) { onClick() },
        color = when {
            isGranted -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            isDenied -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        },
        shape = MawaqitTheme.appShapes.medium
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isGranted -> MaterialTheme.colorScheme.primary
                            isDenied -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        painter = painterResource(
                            when {
                                isGranted -> R.drawable.ic_check
                                isDenied -> R.drawable.ic_error
                                else -> iconRes
                            }
                        ),
                        contentDescription = null,
                        tint = when {
                            isGranted || isDenied -> MaterialTheme.colorScheme.onPrimary
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isGranted -> MaterialTheme.colorScheme.primary
                        isDenied -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }

            if (!isGranted && !isLoading) {
                Icon(
                    painter = painterResource(if (isDenied) R.drawable.ic_close else R.drawable.ic_chevron_right),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isDenied) MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }
        }
    }
}

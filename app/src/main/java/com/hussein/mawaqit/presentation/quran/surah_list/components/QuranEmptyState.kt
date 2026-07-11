package com.hussein.mawaqit.presentation.quran.surah_list.components

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hussein.mawaqit.data.db.entities.TafsirSourceEntity
import com.hussein.mawaqit.presentation.shared.SettingPickerRow
import com.hussein.mawaqit.presentation.shared.SyncStatus
import com.hussein.mawaqit.ui.theme.MawaqitTheme

@Composable
fun QuranEmptyState(
    syncStatus: SyncStatus,
    availableTafsirSources: List<TafsirSourceEntity>,
    selectedTafsirSource:TafsirSourceEntity?,
    onTafsirSourceSelected: (TafsirSourceEntity) -> Unit,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                modifier = Modifier.size(110.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = ImageVector.vectorResource(com.hussein.mawaqit.R.drawable.ic_new_logo),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text = "Welcome to Quran",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Choose your preferences to start your journey.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(32.dp))

            // Selection Rows
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                SettingPickerRow(
                    label = "Quran Language",
                    currentValue = "Arabic",
                    options = listOf("Arabic"),
                    onOptionSelected = { /* Hardcoded for now */ },
                    icon = ImageVector.vectorResource(com.hussein.mawaqit.R.drawable.ic_quran),
                    shape = MawaqitTheme.listShapes.topItem
                )
                SettingPickerRow(
                    label = "Tafsir Source",
                    currentValue = selectedTafsirSource?.name ?: "Select Tafsir",
                    options = availableTafsirSources.map { it.name },
                    onOptionSelected = { name ->
                        availableTafsirSources.find { it.name == name }
                            ?.let { onTafsirSourceSelected(it) }
                    },
                    icon = ImageVector.vectorResource(com.hussein.mawaqit.R.drawable.ic_reciter),
                    shape = MawaqitTheme.listShapes.bottomItem
                )
            }

            Spacer(Modifier.height(40.dp))

            if (syncStatus.isSyncing) {
                SyncProgressBanner(
                    progress = syncStatus.progress,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Button (
                    onClick = onSyncClick,
                    enabled = selectedTafsirSource != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(com.hussein.mawaqit.R.drawable.ic_download),
                        contentDescription = null
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Download Quran Data",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (syncStatus.error != null) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = syncStatus.error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncProgressBanner(
    progress: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Updating Quran Data...",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                strokeCap = StrokeCap.Round
            )
        }
    }
}

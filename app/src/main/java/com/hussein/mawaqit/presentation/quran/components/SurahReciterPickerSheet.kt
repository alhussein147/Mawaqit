package com.hussein.mawaqit.presentation.quran.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.hussein.mawaqit.R
import com.hussein.mawaqit.data.quran.recitation.FullSurahReciter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahReciterPickerSheet(
    current: FullSurahReciter,
    onSelect: (FullSurahReciter) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        SurahReciterPickerSheetContent(
            selectedReciter = current,
            onDismiss = onDismiss,
            onSelect = onSelect
        )
    }
}

@Composable
fun SurahReciterPickerSheetContent(
    selectedReciter: FullSurahReciter,
    onDismiss: () -> Unit,
    onSelect: (FullSurahReciter) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Pick Reciter",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
        )
        FullSurahReciter.entries.forEach { reciter ->
            val isSelected = reciter == selectedReciter
            Surface(
                shape = RoundedCornerShape(16.dp),
                onClick = { onSelect(reciter); onDismiss() },
                modifier = Modifier.fillMaxWidth(),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = reciter.nameArabic,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = reciter.nameEnglish,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_check),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

}
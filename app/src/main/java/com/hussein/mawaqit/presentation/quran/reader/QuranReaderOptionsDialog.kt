package com.hussein.mawaqit.presentation.quran.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.hussein.mawaqit.R
import com.hussein.mawaqit.data.quran.QuranFontSize
import com.hussein.mawaqit.data.quran.QuranTextAlignment

@Composable
fun QuranReaderOptionsDialog(
    selectedFontSize: QuranFontSize,
    selectedTextAlignment: QuranTextAlignment,
    onSelectedFontSize: (QuranFontSize) -> Unit,
    onSelectedTextAlignment: (QuranTextAlignment) -> Unit,
    onDismiss: () -> Unit

) {

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Quran Reader Options",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Quran text alignment",
                    style = MaterialTheme.typography.titleMedium
                )
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth(),
                    space = 0.dp
                ) {
                    QuranTextAlignment.entries.forEach {
                        SegmentedButton(
                            selected = it == selectedTextAlignment,
                            onClick = { onSelectedTextAlignment(it) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = QuranTextAlignment.entries.indexOf(it),
                                count = QuranTextAlignment.entries.size,
                            ), label = {
                                Icon(
                                    imageVector = ImageVector.vectorResource(
                                        when (it) {
                                            QuranTextAlignment.Start -> R.drawable.ic_align_end
                                            QuranTextAlignment.Center -> R.drawable.ic_align_center
                                            QuranTextAlignment.End -> R.drawable.ic_align_start
                                        }
                                    ), contentDescription = null
                                )
                            }
                        )

                    }
                }

                Text(text = "Quran font size", style = MaterialTheme.typography.titleMedium)
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth(),
                    space = 0.dp
                ) {
                    QuranFontSize.entries.forEach {
                        SegmentedButton(
                            selected = it == selectedFontSize,
                            onClick = { onSelectedFontSize(it) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = QuranFontSize.entries.indexOf(it),
                                count = QuranFontSize.entries.size,
                            ), label = {
                                Text(text = it.label)
                            }
                        )

                    }
                }

            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}


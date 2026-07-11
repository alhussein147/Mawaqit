package com.hussein.mawaqit.presentation.quran.reading.reading_settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hussein.mawaqit.R
import com.hussein.mawaqit.infrastructure.settings.QuranTextAlignment
import com.hussein.mawaqit.presentation.shared.BackButton
import com.hussein.mawaqit.presentation.shared.SettingPickerRow
import com.hussein.mawaqit.presentation.shared.SettingSectionHeader
import com.hussein.mawaqit.presentation.shared.TafsirSourceSettingRow
import com.hussein.mawaqit.ui.theme.MawaqitTheme
import com.hussein.mawaqit.ui.theme.quranFontFamily
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun QuranReadingSettingsScreen(
    onBack: () -> Unit,
    viewModel: QuranSettingsViewModel = koinViewModel()
) {
    val fontSize by viewModel.fontSize.collectAsStateWithLifecycle()
    val textAlignment by viewModel.textAlignment.collectAsStateWithLifecycle()
    val availableTafsirSources by viewModel.availableTafsirSources.collectAsStateWithLifecycle()
    val selectedTafsirSourceId by viewModel.selectedTafsirSourceId.collectAsStateWithLifecycle()
    val quranLanguages by viewModel.quranLanguages.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val downloadingSources by viewModel.downloadingSources.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        topBar = {
            LargeTopAppBar(scrollBehavior = scrollBehavior,
                title = {
                    Text(
                        stringResource(R.string.quran_reading_settings),
                        fontWeight = FontWeight.Black
                    )
                },
                navigationIcon = { BackButton(onClick = onBack) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier.padding(paddingValues).nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                start = 16.dp , end = 16.dp, bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.quran_reading_preview),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.basmallah),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = fontSize.sp,
                                fontFamily = quranFontFamily,
                                textAlign = when (textAlignment) {
                                    QuranTextAlignment.Start -> TextAlign.Start
                                    QuranTextAlignment.Center -> TextAlign.Center
                                    QuranTextAlignment.End -> TextAlign.End
                                },
                                textDirection = TextDirection.Rtl,
                                lineHeight = (fontSize * 1.8).sp
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                SettingSectionHeader(stringResource(R.string.font_size))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("A", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Slider(
                            value = fontSize,
                            onValueChange = { viewModel.setFontSize(it) },
                            valueRange = 18f..42f,
                            modifier = Modifier.weight(1f)
                        )
                        Text("A", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                SettingSectionHeader(stringResource(R.string.text_alignment))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuranTextAlignment.entries.forEach { alignment ->
                            val isSelected = textAlignment == alignment
                            IconToggleButton(
                                checked = isSelected,
                                onCheckedChange = { if (it) viewModel.setTextAlignment(alignment) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(
                                                when (alignment) {
                                                    QuranTextAlignment.Start -> R.drawable.ic_align_end
                                                    QuranTextAlignment.Center -> R.drawable.ic_align_center
                                                    QuranTextAlignment.End -> R.drawable.ic_align_start
                                                }
                                            ),
                                            contentDescription = alignment.displayName,
                                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                SettingSectionHeader(title = "Source Settings")

                TafsirSourceSettingRow(
                    shape = MawaqitTheme.listShapes.topItem,
                    selectedSourceId = selectedTafsirSourceId,
                    availableSources = availableTafsirSources,
                    downloadingSources = downloadingSources,
                    onSelect = { viewModel.selectTafsirSource(it) }
                )

                // Language Picker
                SettingPickerRow(
                    shape = MawaqitTheme.listShapes.bottomItem,
                    label = "Quran Language",
                    currentValue = selectedLanguage,
                    options = quranLanguages,
                    onOptionSelected = { viewModel.selectLanguage(it) }
                )

            }

        }
    }
}

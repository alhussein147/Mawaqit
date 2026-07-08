package com.hussein.mawaqit.presentation.quran.reader

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.hussein.mawaqit.ui.theme.quranFontFamily
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun QuranReaderSettingsScreen(
    onBack: () -> Unit,
    viewModel: QuranSettingsViewModel = koinViewModel()
) {
    val fontSize by viewModel.fontSize.collectAsStateWithLifecycle()
    val textAlignment by viewModel.textAlignment.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Reader Settings", fontWeight = FontWeight.Black) },
                navigationIcon = { BackButton(onClick = onBack) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Preview Card
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
                        text = "Preview",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ",
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

            // Font Size Section
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Font Size",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
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

            // Text Alignment Section (Expressive Row)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Text Alignment",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
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
        }
    }
}

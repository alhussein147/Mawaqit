package com.hussein.mawaqit.presentation.quran

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hussein.mawaqit.data.quran.QuranData
import com.hussein.mawaqit.data.quran.Surah

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahListScreen(
    onSurahSelected: (surahIndex: Int) -> Unit,
    onBack: () -> Unit,
    viewModel: QuranViewModel = viewModel()
) {
    val bookmark by viewModel.bookmark.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("القرآن الكريم") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = ImageVector.vectorResource(com.hussein.mawaqit.R.drawable.ic_arrow_back),
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            itemsIndexed(QuranData.surahs, key = { _, surah -> surah.number }) { _, surah ->
                val isBookmarked = bookmark?.surahIndex == surah.number
                SurahRow(
                    surah = surah,
                    isBookmarked = isBookmarked,
                    onClick = { onSurahSelected(surah.number) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
private fun SurahRow(
    surah: Surah,
    isBookmarked: Boolean,
    onClick: () -> Unit
) {
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Surah number badge
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = surah.number.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            // Names
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = surah.nameTransliterated,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Pages ${surah.startPage} – ${QuranData.endPageOf(surah)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Arabic name
            Text(
                text = surah.nameArabic,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.End,
                color = MaterialTheme.colorScheme.primary
            )

            // Bookmark indicator
            if (isBookmarked) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = ImageVector.vectorResource(com.hussein.mawaqit.R.drawable.ic_bookmark_filled),
                    contentDescription = "Bookmarked",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
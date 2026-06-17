package com.hussein.mawaqit.presentation.azkar.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hussein.mawaqit.R
import com.hussein.mawaqit.data.azkar.Zikr
import com.hussein.mawaqit.presentation.azkar.AzkarViewModel
import com.hussein.mawaqit.presentation.shared.BackButton
import com.hussein.mawaqit.presentation.shared.LoadingContent
import com.hussein.mawaqit.ui.theme.quranFontFamily
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AzkarCategoryScreen(
    onCategorySelected: (index: Int) -> Unit,
    viewModel: AzkarViewModel = koinViewModel(),
    onBack: () -> Unit
) {
    val categories by viewModel.categoryTitles.collectAsStateWithLifecycle()
    val isLoading = categories.isEmpty()
    val featuredCategories = categories.take(3).mapIndexed { index, title -> index to title }
    val remainingCategories = categories.drop(3).mapIndexed { index, title -> index + 3 to title }

    Scaffold(
        modifier = Modifier,
        topBar = {
            TopAppBar(
                title = { Text("Azkar") },
                navigationIcon = {
                    BackButton(onClick = onBack)
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            LoadingContent(modifier = Modifier.fillMaxSize())
        } else {

            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp) ,
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                item {
                    FeaturedAzkarSection(
                        categories = featuredCategories,
                        onCategorySelected = onCategorySelected
                    )
                }
                item {
                    Text("All Azkar")
                }
                items(remainingCategories, key = { it.first }) { (index, category) ->
                    CategoryListItem(
                        index = index,
                        title = category,
                        onClick = { onCategorySelected(index) }
                    )
                }
            }
        }
    }
}


@Composable
private fun FeaturedAzkarSection(
    categories: List<Pair<Int, String>>,
    onCategorySelected: (index: Int) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
        contentPadding =PaddingValues(horizontal = 20.dp)
    ) {
        items(categories, key = { it.first }) { (index, title) ->
            FeaturedAzkarCard(
                title = title,
                label = when (index) {
                    0 -> "Evening"
                    1 -> "Morning"
                    2 -> "After prayer"
                    else -> "Common"
                },
                onClick = { onCategorySelected(index) }
            )
        }
    }
}

@Composable
private fun FeaturedAzkarCard(
    title: String,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(210.dp)
            .height(132.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.76f)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CategoryListItem(
    index: Int,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_placeholder),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Rtl),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "Collection ${index + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(50.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Text(
                    text = (index + 1).toString(),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

        }
    }
}

@Composable
fun ZikrItem(index: Int, zikr: Zikr) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Index badge
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                text = index.toString(),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(Modifier.height(10.dp))

        // Arabic zikr text — RTL
        Text(
            fontFamily = quranFontFamily,
            text = zikr.text,
            style = MaterialTheme.typography.bodyLarge.copy(
                lineHeight = 32.sp,
                fontSize = 18.sp
            ),
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
        )

        // Repeat count — only show if > 1
        if (zikr.repeat > 1) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Repeat ${zikr.repeat}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )
        }
    }
}

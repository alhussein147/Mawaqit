package com.hussein.mawaqit.presentation.azkar

import android.R.attr.category
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.common.collect.Multimaps.index
import com.hussein.mawaqit.data.azkar.Zikr
import com.hussein.mawaqit.presentation.shared.LoadingContent
import com.hussein.mawaqit.ui.theme.quranFontFamily
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AzkarCategoryScreen(
    onCategorySelected: (index: Int) -> Unit,
    onBack: () -> Unit,
    viewModel: AzkarViewModel = koinViewModel()
) {
    val categories by viewModel.categoryTitles.collectAsStateWithLifecycle()
    val isLoading = categories.isEmpty()


        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Azkar") },
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
            if (isLoading) {
                LoadingContent()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    itemsIndexed(categories) { index, category ->
                        CategoryCard(
                            title = category,
                            onClick = { onCategorySelected(index) }
                        )
                    }
                }
            }
        }
    }



@Composable
private fun CategoryCard(title: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.size(12.dp))

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
        if (zikr.count > 1) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Repeat ${zikr.count}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )
        }
    }
}

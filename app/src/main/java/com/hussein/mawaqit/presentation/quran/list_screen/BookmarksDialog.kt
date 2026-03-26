package com.hussein.mawaqit.presentation.quran.list_screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hussein.mawaqit.data.db.BookmarkEntity
import com.hussein.mawaqit.data.quran.QuranData


@Composable
 fun BookmarksDialog(
    bookmarks: List<BookmarkEntity>,
    onNavigate: (surahNumber: Int, ayahNumber: Int) -> Unit,
    onDelete: (surahNumber: Int, ayahNumber: Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Bookmarks",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            if (bookmarks.isEmpty()) {
                Text(
                    text = "Empty",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(bookmarks, key = { "${it.surahNumber}_${it.ayahNumber}" }) { bookmark ->
                        val surah = QuranData.surahs.getOrNull(bookmark.surahNumber - 1)
                        BookmarkItem(
                            surahName = surah?.nameArabic ?: "",
                            ayahNumber = bookmark.ayahNumber,
                            onClick = { onNavigate(bookmark.surahNumber, bookmark.ayahNumber) },
                            onDelete = { onDelete(bookmark.surahNumber, bookmark.ayahNumber) }
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

@Composable
private fun BookmarkItem(
    surahName: String,
    ayahNumber: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Delete button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(com.hussein.mawaqit.R.drawable.ic_placeholder),
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Surah name + ayah number
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = surahName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Ayah $ayahNumber",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
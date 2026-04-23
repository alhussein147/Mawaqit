package com.hussein.mawaqit.presentation.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        Modifier
            .background(color = MaterialTheme.colorScheme.background).fillMaxWidth()
            .then(modifier),
        contentAlignment = Alignment.Center
    ) { ContainedLoadingIndicator() }
}


@Composable
fun ErrorContent(message: String, modifier: Modifier = Modifier) {
    Box(
        Modifier
            .background(color = MaterialTheme.colorScheme.background).fillMaxWidth().then(modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
    }
}
package com.hussein.mawaqit.presentation.azkar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hussein.mawaqit.presentation.azkar.components.ZikrItem
import com.hussein.mawaqit.presentation.shared.BackButton
import com.hussein.mawaqit.presentation.shared.ErrorContent
import com.hussein.mawaqit.presentation.shared.LoadingContent
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AzkarScreen(
    categoryIndex: Int,
    onBack: () -> Unit,
    viewModel: AzkarViewModel = koinViewModel(),
) {
    val listState by viewModel.listState.collectAsStateWithLifecycle()

    LaunchedEffect(categoryIndex) {
        viewModel.selectCategory(categoryIndex)
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        if (listState is AzkarListState.Success) (listState as AzkarListState.Success).category.title else "Azkar"
                    )
                },
                navigationIcon = {
                    BackButton(onClick = onBack)
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        when (val s = listState) {
            AzkarListState.Loading -> {
                LoadingContent(modifier = Modifier.fillMaxSize().padding(padding))
            }

            is AzkarListState.Error -> {
                ErrorContent(message = s.message, modifier = Modifier.fillMaxSize().padding(padding))
            }

            is AzkarListState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = padding.calculateTopPadding() + 8.dp,
                        bottom = padding.calculateBottomPadding() + 32.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(
                        s.category.content,
                        key = { index, _ -> index }
                    ) { index, zikr ->
                        ZikrItem(index = index + 1, zikr = zikr)
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }

            else -> {}
        }
    }
}

package com.hussein.mawaqit.presentation.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp

@Composable
fun FloatingNavBar(
    modifier: Modifier = Modifier,
    items: List<TopLevelDestination>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    show: Boolean
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = show,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            shadowElevation = 6.dp,
            modifier = Modifier.padding(16.dp).windowInsetsPadding(
                WindowInsets.navigationBars
            )
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items.forEachIndexed { index, item ->
                    FloatingNavItem(
                        icon = { Icon(ImageVector.vectorResource(item.icon), null) },
                        title = item.label,
                        selected = index == selectedIndex,
                        onClick = { onSelect(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingNavItem(
    icon: @Composable () -> Unit,
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        tonalElevation = if (selected) 8.dp else 2.dp
    ) {
        Row(
            modifier = Modifier
                .height(48.dp)
                .animateContentSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(12.dp))
            icon()

            AnimatedVisibility(
                visible = selected,
                enter = expandHorizontally(animationSpec = tween(durationMillis = 150)) + fadeIn(animationSpec = tween(durationMillis = 150)),
                exit = shrinkHorizontally(animationSpec = tween(durationMillis = 150)) + fadeOut(animationSpec = tween(durationMillis = 150))
            ) {
                Text(
                    text = title,
                    modifier = Modifier.padding(horizontal = 6.dp),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(Modifier.width(12.dp))
        }
    }
}


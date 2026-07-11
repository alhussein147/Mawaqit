package com.hussein.mawaqit.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

@Immutable
data class ListShapes(
    val topItem: RoundedCornerShape = RoundedCornerShape(
        topStart = 24.dp,
        topEnd = 24.dp,
        bottomEnd = 2.dp,
        bottomStart = 2.dp
    ),
    val midItem: RoundedCornerShape = RoundedCornerShape(
        topStart = 2.dp,
        topEnd = 2.dp,
        bottomEnd = 2.dp,
        bottomStart = 2.dp
    ),
    val bottomItem: RoundedCornerShape = RoundedCornerShape(
        topStart = 2.dp,
        topEnd = 2.dp,
        bottomEnd = 24.dp,
        bottomStart = 24.dp
    )
)

val independentListItemShape = RoundedCornerShape(28.dp)

val LocalListItemShape = staticCompositionLocalOf { independentListItemShape }
val LocalListShapes = staticCompositionLocalOf { ListShapes() }

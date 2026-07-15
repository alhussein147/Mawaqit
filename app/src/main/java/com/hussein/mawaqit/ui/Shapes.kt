package com.hussein.mawaqit.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

@Immutable
data class ListShapes(
    val topItem: RoundedCornerShape = RoundedCornerShape(
        topStart = 28.dp,
        topEnd = 28.dp,
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
        bottomEnd = 28.dp,
        bottomStart = 28.dp
    )
)



data class AppShapes(
    val small: RoundedCornerShape = RoundedCornerShape(16.dp),
    val medium: RoundedCornerShape = RoundedCornerShape(24.dp),
    val large: RoundedCornerShape = RoundedCornerShape(28.dp),
    val circle: RoundedCornerShape = RoundedCornerShape(50),
    val listShapes: ListShapes = ListShapes()
)

val LocalAppShapes = staticCompositionLocalOf { AppShapes() }

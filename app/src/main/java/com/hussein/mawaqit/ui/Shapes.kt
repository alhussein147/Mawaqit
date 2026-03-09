package com.hussein.mawaqit.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

val listShapes = ListShapes()
data class ListShapes(
    val topItem: RoundedCornerShape = RoundedCornerShape(
        12.dp, 12.dp, 2.dp, 2.dp
    ),
    val midItem: RoundedCornerShape = RoundedCornerShape(2.dp, 2.dp, 2.dp, 2.dp),
    val bottomItem: RoundedCornerShape = RoundedCornerShape(2.dp, 2.dp, 12.dp, 12.dp)
)
package com.hussein.mawait.core.data.models

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class Prayer @OptIn(ExperimentalTime::class) constructor(
    val name: String,
    val time: Instant
)
package com.hussein.mawaqit.presentation.home.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hussein.mawaqit.presentation.home.PrayerStatus
import com.hussein.mawaqit.presentation.home.PrayerUiModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val ARC_SWEEP_DEG = 70f
private const val ARC_START_DEG = 180f + (180f - ARC_SWEEP_DEG) / 2f
private const val ARC_HEIGHT_FRACTION = 0.5f   // how high the arc peak sits (0 = bottom, 1 = top)

private fun nodePosition(index: Int, total: Int, width: Float, height: Float): Offset {
    val fraction = index.toFloat() / (total - 1).toFloat()
    val angleDeg = ARC_START_DEG + ARC_SWEEP_DEG * fraction
    val angleRad = Math.toRadians(angleDeg.toDouble())
    val peakY = height * (1f - ARC_HEIGHT_FRACTION)
    val radius = (width / 2f) / sin(Math.toRadians(ARC_SWEEP_DEG / 2.0)).toFloat()
    val centerX = width / 2f
    val centerY = peakY + radius
    return Offset(
        x = (centerX + radius * cos(angleRad)).toFloat(),
        y = (centerY + radius * sin(angleRad)).toFloat()
    )
}

@OptIn(ExperimentalTime::class)
@Composable
fun PrayerArchStepper(
    prayers: List<PrayerUiModel>,
    modifier: Modifier = Modifier,
    now: kotlin.time.Instant = Clock.System.now()
) {
    if (prayers.isEmpty()) return

    val primary = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val surface = MaterialTheme.colorScheme.surface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val nameStyleBase =
        MaterialTheme.typography.labelSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
    val timeStyleBase = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)

    val textMeasurer = rememberTextMeasurer()

    val nameMeasured = remember(prayers) {
        prayers.map { prayer ->
            textMeasurer.measure(prayer.name, nameStyleBase)
        }
    }
    val timeMeasured = remember(prayers) {
        prayers.map { prayer ->
            textMeasurer.measure(prayer.time.formatTime(), timeStyleBase)
        }
    }

    val currentIndex = prayers.indexOfFirst { it.status == PrayerStatus.CURRENT }

    val targetProgress = when {
        prayers.all { it.status == PrayerStatus.PASSED } -> 1f
        prayers.all { it.status == PrayerStatus.UPCOMING } -> 0f
        currentIndex >= 0 -> {
            // Base: how far along the node sequence we are (e.g. 2/4 for the 3rd node)
            val nodeProgress = currentIndex.toFloat() / (prayers.size - 1).toFloat()
            // Segment width between two adjacent nodes in progress-space
            val segmentWidth = 1f / (prayers.size - 1).toFloat()

            // Interpolate within the current window if there is a next prayer
            val currentPrayer = prayers[currentIndex]
            val nextPrayer = prayers.getOrNull(currentIndex + 1)

            if (nextPrayer != null) {
                val windowMs = (nextPrayer.time - currentPrayer.time).inWholeMilliseconds.toFloat()
                val elapsedMs = (now - currentPrayer.time).inWholeMilliseconds.toFloat()
                val fraction = (elapsedMs / windowMs).coerceIn(0f, 1f)
                nodeProgress + segmentWidth * fraction
            } else {
                // Isha is current — arc is fully filled
                1f
            }
        }

        else -> 0f
    }
    val progress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "arch_progress"
    )

    BoxWithConstraints(
        modifier = modifier
    ) {

        val calculatedHeight = (maxWidth * 0.35f)

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(calculatedHeight)
                .padding(horizontal = 24.dp)
        ) {
            val w = size.width
            val h = size.height

            // Arc geometry — derived entirely from DrawScope.size
            val peakY = h * (1f - ARC_HEIGHT_FRACTION)
            val radius = (w / 2f) / sin(Math.toRadians(ARC_SWEEP_DEG / 2.0)).toFloat()
            val arcTopLeft = Offset(w / 2f - radius, peakY)
            val arcRect = Size(radius * 2, radius * 2)

            // Node positions
            val positions = prayers.indices.map { i -> nodePosition(i, prayers.size, w, h) }

            // ── Background track ─────────────────────────────────────────────────
            drawArc(
                color = surfaceVariant,
                startAngle = ARC_START_DEG,
                sweepAngle = ARC_SWEEP_DEG,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcRect,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // ── Filled progress segment ──────────────────────────────────────────
            val currentIdx = prayers.indexOfFirst { it.status == PrayerStatus.CURRENT }
            val nextIdx    = if (currentIdx >= 0) currentIdx + 1 else -1

            if (progress > 0f) {
                drawArc(
                    color = primary,
                    startAngle = ARC_START_DEG,
                    sweepAngle = ARC_SWEEP_DEG * progress,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcRect,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
                val tipAngleRad = Math.toRadians((ARC_START_DEG + ARC_SWEEP_DEG * progress).toDouble())
                val tipPos = Offset(
                    x = (size.width / 2f + radius * cos(tipAngleRad)).toFloat(),
                    y = (peakY + radius + radius * sin(tipAngleRad)).toFloat()
                )
                drawCircle(color = primary, radius = 4.dp.toPx(), center = tipPos)


            }

            // ── Nodes + labels ───────────────────────────────────────────────────
            prayers.forEachIndexed { i, prayer ->
                val pos = positions[i]
                val isCurrent = prayer.status == PrayerStatus.CURRENT
                val labelColor = if (isCurrent) primary else onSurfaceVariant
                val isNext = i == nextIdx


                // Node
                drawNode(
                    pos = pos,
                    status = prayer.status,
                    primary = primary,
                    surface = surface,
                    surfaceVariant = surfaceVariant, isNext =isNext
                )

                // Prayer name — centred above the node
                val nameResult = nameMeasured[i]
                val nameX = pos.x - nameResult.size.width / 2f
                val nameY = pos.y - nameResult.size.height - 10.dp.toPx()

                drawText(
                    textLayoutResult = nameResult,
                    color = labelColor,
                    topLeft = Offset(nameX, nameY)
                )

                // Prayer time — centred below the node
                val timeResult = timeMeasured[i]
                val timeX = pos.x - timeResult.size.width / 2f
                val timeY = pos.y + 10.dp.toPx()
                drawText(
                    textLayoutResult = timeResult,
                    color = labelColor.copy(alpha = 0.7f),
                    topLeft = Offset(timeX, timeY)
                )
            }
        }
    }
}

private fun DrawScope.drawNode(
    pos: Offset,
    status: PrayerStatus,
    isNext: Boolean,
    primary: Color,
    surface: Color,
    surfaceVariant: Color
) {
    when {
        status == PrayerStatus.PASSED -> {
            drawCircle(color = primary, radius = 5.dp.toPx(), center = pos)
        }

        status == PrayerStatus.CURRENT -> {
            // Glow via shadow layer on a transparent circle
            drawIntoCanvas { canvas ->
                val glowPaint = Paint().apply {
                    asFrameworkPaint().apply {
                        isAntiAlias = true
                        color = android.graphics.Color.TRANSPARENT
                        setShadowLayer(18f, 0f, 0f, primary.copy(alpha = 0.45f).toArgb())
                    }
                }
                canvas.drawCircle(pos, 13.dp.toPx(), glowPaint)
            }
            drawCircle(color = primary.copy(alpha = 0.18f), radius = 13.dp.toPx(), center = pos)
            drawCircle(color = primary, radius = 8.dp.toPx(), center = pos)
            drawCircle(color = surface, radius = 3.dp.toPx(), center = pos)
        }

        isNext -> {
            // Destination node — outlined ring in primary color showing where arc is heading
            drawCircle(
                color = surfaceVariant,
                radius = 5.dp.toPx(),
                center = pos,
                style = Stroke(width = 1.8.dp.toPx())
            )
        }

        else -> {
            drawCircle(
                color = surfaceVariant,
                radius = 5.dp.toPx(),
                center = pos,
                style = Stroke(width = 1.8.dp.toPx())
            )
        }
    }
}

@OptIn(ExperimentalTime::class)
private fun kotlin.time.Instant.formatTime(): String {
    val local = toLocalDateTime(TimeZone.currentSystemDefault())
    val hour12 = when {
        local.hour == 0 -> 12
        local.hour <= 12 -> local.hour
        else -> local.hour - 12
    }
    return "%d:%02d %s".format(hour12, local.minute, if (local.hour < 12) "AM" else "PM")
}

package com.hussein.mawaqit.presentation.qiblah

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hussein.mawaqit.R
import com.hussein.mawaqit.presentation.shared.BackButton
import com.hussein.mawaqit.ui.theme.MawaqitTheme
import org.koin.androidx.compose.koinViewModel
import kotlin.math.abs
import androidx.compose.ui.graphics.drawscope.rotate as canvasRotate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblahRoute(
    viewModel: QiblahViewModel = koinViewModel(),
    onBack: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    DisposableEffect(Unit) {
        viewModel.start()
        onDispose {
            viewModel.stop()
        }
    }

    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    val topAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    // Smooth, springy animation — signature M3 Expressive motion
    val animatedAngle = remember { Animatable(state.qiblahAngle) }

    LaunchedEffect(state.qiblahAngle) {
        val delta = state.qiblahAngle - (animatedAngle.value % 360)
        val target = animatedAngle.value + when {
            delta > 180 -> delta - 360
            delta < -180 -> delta + 360
            else -> delta
        }
        animatedAngle.animateTo(
            targetValue = target,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium // Faster animation
            )
        )
    }

    val isAligned = abs(((state.qiblahAngle + 180) % 360) - 180) < 5f

    // Vibration feedback when aligned
    LaunchedEffect(isAligned) {
        if (isAligned) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.qiblah), fontWeight = FontWeight.Black) },
                scrollBehavior = topAppBarScrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    navigationIconContentColor = Color.Unspecified,
                    titleContentColor = Color.Unspecified,
                    actionIconContentColor = Color.Unspecified
                ), navigationIcon = {
                    BackButton(onClick = onBack)
                }
            )

        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StatusPill(isAligned)

            if (state.accuracy < 2) {
                AccuracyWarning()
            }

            Surface(
                modifier = Modifier
                    .padding(top = 32.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = CircleShape,
                tonalElevation = 4.dp,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CompassDial(
                        heading = state.compassHeading,
                        isAligned = isAligned
                    )

                    QiblahNeedle(
                        angle = animatedAngle.value,
                        isAligned = isAligned,
                        modifier = Modifier.fillMaxSize(0.75f)
                    )

                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 12.dp,
                        shadowElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_new_logo),
                                contentDescription = null,
                                tint = if (isAligned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            InfoCard(
                heading = state.compassHeading,
                qiblahAngle = state.qiblahAngle,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }


}

@Composable
private fun AccuracyWarning() {
    Surface(
        modifier = Modifier.padding(top = 16.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(com.hussein.mawaqit.R.drawable.ic_error),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Low compass accuracy. Calibrate by moving in ∞ shape.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun StatusPill(isAligned: Boolean) {
    val bg by animateColorAsState(
        targetValue = if (isAligned)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.secondaryContainer,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "pillColor"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isAligned)
            MaterialTheme.colorScheme.onPrimary
        else
            MaterialTheme.colorScheme.onSecondaryContainer,
        label = "contentColor"
    )

    Surface(
        color = bg,
        contentColor = contentColor,
        shape = MawaqitTheme.appShapes.circle,
        shadowElevation = if (isAligned) 12.dp else 2.dp,
        modifier = Modifier.graphicsLayer {
            val s = if (isAligned) 1.08f else 1f
            scaleX = s
            scaleY = s
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isAligned) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_check),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = if (isAligned) "Facing the Qiblah" else "Rotate to find Qiblah",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun CompassDial(heading: Float, isAligned: Boolean) {
    val ringColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val tickColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val cardinalColor = MaterialTheme.colorScheme.primary
    val alignedColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .rotate(-heading)
    ) {
        val radius = size.minDimension / 2
        
        // Outer ring
        drawCircle(
            color = if (isAligned) alignedColor.copy(alpha = 0.1f) else ringColor,
            radius = radius,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )

        // Ticks
        for (deg in 0 until 360 step 5) {
            canvasRotate(deg.toFloat()) {
                val isCardinal = deg % 90 == 0
                val isMajor = deg % 30 == 0
                
                val tickLength = when {
                    isCardinal -> 24.dp.toPx()
                    isMajor -> 16.dp.toPx()
                    else -> 8.dp.toPx()
                }
                
                val color = when {
                    isCardinal -> cardinalColor
                    isMajor -> tickColor.copy(alpha = 0.8f)
                    else -> tickColor
                }

                drawLine(
                    color = color,
                    start = Offset(center.x, center.y - radius),
                    end = Offset(center.x, center.y - radius + tickLength),
                    strokeWidth = if (isMajor) 3.dp.toPx() else 1.dp.toPx()
                )
            }
        }
    }
}

@Composable
private fun QiblahNeedle(angle: Float, isAligned: Boolean, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val onSurface = MaterialTheme.colorScheme.onSurface

    val scale by animateFloatAsState(
        targetValue = if (isAligned) 1.2f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "needleScale"
    )

    Box(
        modifier = modifier
            .rotate(angle)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(if (isAligned) 32.dp else 12.dp, CircleShape, clip = false),
        contentAlignment = Alignment.TopCenter
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val needleWidth = w * 0.16f

            if (isAligned) {
                drawCircle(
                    color = primary.copy(alpha = 0.15f),
                    radius = w / 2,
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Fill
                )
            }

            // Main needle body (Kaaba pointing)
            drawPath(
                path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w / 2, 0f)
                    lineTo(w / 2 - needleWidth, h / 2)
                    lineTo(w / 2 + needleWidth, h / 2)
                    close()
                },
                brush = Brush.verticalGradient(
                    colors = listOf(primary, tertiary),
                    startY = 0f,
                    endY = h / 2
                )
            )
            
            // Tail part
            drawPath(
                path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w / 2, h)
                    lineTo(w / 2 - needleWidth * 0.5f, h / 2)
                    lineTo(w / 2 + needleWidth * 0.5f, h / 2)
                    close()
                },
                color = onSurface.copy(alpha = 0.1f)
            )
            
            // Central pin decoration
            drawCircle(
                color = Color.White,
                radius = 4.dp.toPx(),
                center = center
            )
        }
    }
}

@Composable
private fun InfoCard(heading: Float, qiblahAngle: Float, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MawaqitTheme.appShapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InfoBox(
                label = "Compass",
                value = "${heading.toInt()}°",
                modifier = Modifier.weight(1f)
            )
            InfoBox(
                label = "Qiblah",
                value = "${qiblahAngle.toInt()}°",
                modifier = Modifier.weight(1f),
                isAccent = true
            )
        }
    }
}

@Composable
private fun InfoBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isAccent: Boolean = false
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            color = if (isAccent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            shape = MawaqitTheme.appShapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = value,
                modifier = Modifier.padding(vertical = 12.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = if (isAccent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}


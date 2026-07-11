package com.hussein.mawaqit.presentation.onboarding.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hussein.mawaqit.R

@Composable
fun PageContent(
    iconRes: Int,
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    progress: Float? = null,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(40.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            lineHeight = 44.sp
        )

        subtitle?.let {
            Spacer(Modifier.height(16.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 26.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        progress?.let {
            Spacer(Modifier.height(32.dp))
            LinearProgressIndicator(
                progress = { it },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(12.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer,
                strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
            )
        }

        errorMessage?.takeIf { it.isNotBlank() }?.let { msg ->
            Spacer(Modifier.height(24.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    text = msg,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun OnboardingActions(
    page: OnboardingPage,
    onPrimaryClick: () -> Unit,
    onSkipClick: () -> Unit,
    modifier: Modifier = Modifier,
    primaryButtonEnabled: Boolean = true
) {
    val primaryLabel = when (page) {
        OnboardingPage.WELCOME -> stringResource(R.string.get_started)
        OnboardingPage.PERMISSIONS -> stringResource(R.string.continue_)
        OnboardingPage.OPTIONS -> stringResource(R.string.continue_)
//        OnboardingPage.QURAN_SETUP -> stringResource(R.string.load_quran_data)
        OnboardingPage.DONE -> stringResource(R.string.finish)
    }

//    val showSkip = page == OnboardingPage.QURAN_SETUP

    Column(
        modifier = modifier
            .background(Color.Transparent)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onPrimaryClick,
            enabled = primaryButtonEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(32.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Text(
                primaryLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

//        if (showSkip) {
//            Spacer(Modifier.height(12.dp))
//            TextButton(
//                onClick = onSkipClick,
//                modifier = Modifier.height(48.dp)
//            ) {
//                Text(
//                    stringResource(R.string.skip_for_now),
//                    style = MaterialTheme.typography.labelLarge,
//                    color = MaterialTheme.colorScheme.primary
//                )
//            }
//        }
    }
}

@Composable
fun StepIndicator(
    currentPage: OnboardingPage,
    modifier: Modifier = Modifier
) {
    val visiblePages = OnboardingPage.entries
    val activeIndex = visiblePages.indexOf(currentPage).coerceAtLeast(0)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        visiblePages.forEachIndexed { index, _ ->
            val isActive = index == activeIndex
            val width by animateFloatAsState(
                targetValue = if (isActive) 32f else 10f,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
                label = "indicator_width"
            )

            Box(
                modifier = Modifier
                    .height(10.dp)
                    .width(width.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
            )
        }
    }
}

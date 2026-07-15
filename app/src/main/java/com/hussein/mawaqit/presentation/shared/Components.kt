package com.hussein.mawaqit.presentation.shared

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.hussein.mawaqit.R

@Composable
fun BackButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    @DrawableRes icon: Int = R.drawable.ic_arrow_back,
) {
    FilledTonalIconButton(
        modifier = Modifier.size(IconButtonDefaults.smallContainerSize()).then(modifier),
        onClick = onClick,
        shapes = IconButtonDefaults.shapes(
            shape = IconButtonDefaults.filledShape,
            pressedShape = IconButtonDefaults.mediumPressedShape
        )
    ) {
        Icon(
            modifier = Modifier.size(IconButtonDefaults.mediumIconSize),
            imageVector = ImageVector.vectorResource(icon),
            contentDescription = stringResource(R.string.settings)
        )
    }

}

@Composable
fun RootScreenWrapper(
    modifier: Modifier = Modifier,
    topAppBar: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(modifier)
        ) {
            topAppBar()

            Box(
                Modifier
                    .weight(1f)
                    .consumeWindowInsets(WindowInsets.statusBars)
            ) {
                content()
            }

            bottomBar()
        }
    }
}

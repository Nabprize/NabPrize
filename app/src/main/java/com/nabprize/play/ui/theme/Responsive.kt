package com.nabprize.play.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class ResponsiveMetrics(
    val isCompact: Boolean,
    val horizontalPadding: Dp,
    val cardPadding: Dp,
    val illustrationSize: Dp,
    val statCardHeight: Dp
)

@Composable
fun rememberResponsiveMetrics(): ResponsiveMetrics {
    val width = LocalConfiguration.current.screenWidthDp
    return remember(width) {
        val compact = width < 340
        ResponsiveMetrics(
            isCompact = compact,
            horizontalPadding = if (compact) 14.dp else 20.dp,
            cardPadding = if (compact) 16.dp else 20.dp,
            illustrationSize = if (compact) 92.dp else 116.dp,
            statCardHeight = if (compact) 124.dp else 140.dp
        )
    }
}

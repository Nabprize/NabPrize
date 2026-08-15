package com.nabprize.play.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nabprize.play.ui.theme.PrimaryOrange
import com.nabprize.play.ui.theme.AccentGold

@Composable
fun GradientProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 12.dp,
    trackColor: Color = PrimaryOrange.copy(alpha = 0.18f),
    progressColor: Brush = Brush.horizontalGradient(
        colors = listOf(PrimaryOrange, AccentGold)
    ),
    enabled: Boolean = true
) {
    val capped = progress.coerceIn(0f, 1f)
    val barColor = if (enabled) progressColor else Brush.horizontalGradient(
        listOf(Color(0xFFBDBDBD), Color(0xFF9E9E9E))
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
    ) {
        val barHeight = this.size.height
        val barRadius = this.size.height / 2f

        // track
        drawRoundRect(
            color = trackColor,
            topLeft = Offset(0f, 0f),
            size = androidx.compose.ui.geometry.Size(this.size.width, barHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barRadius, barRadius)
        )

        // progress
        val progressWidth = this.size.width * capped
        if (progressWidth > 0f) {
            drawRoundRect(
                brush = barColor,
                topLeft = Offset(0f, 0f),
                size = androidx.compose.ui.geometry.Size(progressWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barRadius, barRadius)
            )
        }
    }
}

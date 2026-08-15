package com.nabprize.play.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nabprize.play.ui.theme.AccentGold
import com.nabprize.play.ui.theme.PrimaryOrange
import com.nabprize.play.ui.theme.SecondaryPurple
import com.nabprize.play.ui.theme.TextPrimary
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun OneVsOneIllustration(modifier: Modifier = Modifier, size: Dp = 120.dp) {
    val coral = PrimaryOrange
    val purple = SecondaryPurple
    val gold = AccentGold

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = this.size.width
            val h = this.size.height
            val avatarR = 22.dp.toPx()

            val leftCx = avatarR + 8.dp.toPx()
            val rightCx = w - avatarR - 8.dp.toPx()
            val cy = h * 0.44f

            // ── Lightning bolt / VS bridge ─────────────────────────
            val midX = w / 2f
            // Glow arc between players
            drawArcGlow(
                brush = Brush.horizontalGradient(
                    listOf(coral.copy(0.25f), gold.copy(0.35f), purple.copy(0.25f))
                ),
                cx = midX,
                cy = cy,
                radiusX = (midX - leftCx - avatarR),
                radiusY = 14.dp.toPx(),
                strokeWidth = 3.dp.toPx()
            )

            // ── Left avatar (coral) ────────────────────────────────
            // outer glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(coral.copy(0.35f), Color.Transparent),
                    center = Offset(leftCx, cy),
                    radius = avatarR * 1.7f
                ),
                radius = avatarR * 1.7f,
                center = Offset(leftCx, cy)
            )
            // body circle
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(coral, coral.copy(0.7f)),
                    start = Offset(leftCx - avatarR, cy - avatarR),
                    end = Offset(leftCx + avatarR, cy + avatarR)
                ),
                radius = avatarR,
                center = Offset(leftCx, cy)
            )
            // head
            val headR = avatarR * 0.40f
            drawCircle(
                color = Color(0xFFFFF0E0),
                radius = headR,
                center = Offset(leftCx, cy - avatarR * 0.30f)
            )
            // body stub
            drawCircle(
                color = Color(0xFFFFF0E0).copy(alpha = 0.9f),
                radius = avatarR * 0.30f,
                center = Offset(leftCx, cy + avatarR * 0.52f)
            )
            // shield-like white rim
            drawCircle(
                color = Color.White.copy(alpha = 0.25f),
                radius = avatarR * 0.95f,
                center = Offset(leftCx - avatarR * 0.1f, cy - avatarR * 0.1f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )

            // ── Right avatar (purple) ──────────────────────────────
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(purple.copy(0.35f), Color.Transparent),
                    center = Offset(rightCx, cy),
                    radius = avatarR * 1.7f
                ),
                radius = avatarR * 1.7f,
                center = Offset(rightCx, cy)
            )
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(purple, purple.copy(0.7f)),
                    start = Offset(rightCx - avatarR, cy - avatarR),
                    end = Offset(rightCx + avatarR, cy + avatarR)
                ),
                radius = avatarR,
                center = Offset(rightCx, cy)
            )
            drawCircle(
                color = Color(0xFFFFF0E0),
                radius = headR,
                center = Offset(rightCx, cy - avatarR * 0.30f)
            )
            drawCircle(
                color = Color(0xFFFFF0E0).copy(alpha = 0.9f),
                radius = avatarR * 0.30f,
                center = Offset(rightCx, cy + avatarR * 0.52f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.25f),
                radius = avatarR * 0.95f,
                center = Offset(rightCx + avatarR * 0.1f, cy - avatarR * 0.1f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )

            // ── Sparks between avatars ─────────────────────────────
            val sparkPositions = listOf(0.3f, 0.5f, 0.7f)
            sparkPositions.forEach { t ->
                val sx = leftCx + avatarR + (rightCx - leftCx - avatarR * 2) * t
                val sy = cy + (-1f + t * 2f) * 6.dp.toPx()
                drawCircle(
                    color = gold.copy(alpha = 0.7f),
                    radius = 2.2.dp.toPx(),
                    center = Offset(sx, sy)
                )
            }
        }

        // VS badge centered
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(38.dp, 24.dp)) {
                val vsBg = Brush.linearGradient(
                    colors = listOf(gold, PrimaryOrange),
                    start = Offset(0f, 0f),
                    end = Offset(this.size.width, this.size.height)
                )
                drawRoundRect(
                    brush = vsBg,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
                    size = this.size
                )
            }
            Text(
                text = "VS",
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            )
        }
    }
}

private fun DrawScope.drawArcGlow(
    brush: Brush,
    cx: Float,
    cy: Float,
    radiusX: Float,
    radiusY: Float,
    strokeWidth: Float
) {
    // approximate elliptical arc via line segments
    val steps = 24
    for (i in 0 until steps) {
        val a0 = Math.PI - (Math.PI * i / steps)
        val a1 = Math.PI - (Math.PI * (i + 1) / steps)
        val x0 = (cx + radiusX * cos(a0)).toFloat()
        val y0 = (cy + radiusY * sin(a0)).toFloat()
        val x1 = (cx + radiusX * cos(a1)).toFloat()
        val y1 = (cy + radiusY * sin(a1)).toFloat()
        drawLine(
            brush = brush,
            start = Offset(x0, y0),
            end = Offset(x1, y1),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

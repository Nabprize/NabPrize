package com.nabprize.play.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nabprize.play.ui.theme.AccentGold
import com.nabprize.play.ui.theme.PrimaryOrange
import com.nabprize.play.ui.theme.SecondaryPurple

@Composable
fun DotsAndBoxesPreview(modifier: Modifier = Modifier, size: Dp = 120.dp) {
    val coral = PrimaryOrange
    val purple = SecondaryPurple
    val gold = AccentGold
    val dotColor = Color(0xFF2A2A2A)
    val gridColor = Color(0xFFD8CCB8)

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        // 3x3 grid of cells → 4x4 dots
        val cols = 3
        val rows = 3
        val cellW = w / cols
        val cellH = h / rows
        val strokeLine = (2.8).dp.toPx()
        val strokeActive = (4.2).dp.toPx()
        val dotR = (4.5).dp.toPx()

        // ── Box fills ──────────────────────────────────────────────
        // top-left cell: coral fill
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(coral.copy(alpha = 0.30f), coral.copy(alpha = 0.12f)),
                start = Offset(0f, 0f),
                end = Offset(cellW, cellH)
            ),
            topLeft = Offset(0f, 0f),
            size = Size(cellW, cellH),
            cornerRadius = CornerRadius(6.dp.toPx())
        )
        // center cell: purple fill
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(purple.copy(alpha = 0.28f), purple.copy(alpha = 0.10f)),
                start = Offset(cellW, cellH),
                end = Offset(cellW * 2, cellH * 2)
            ),
            topLeft = Offset(cellW, cellH),
            size = Size(cellW, cellH),
            cornerRadius = CornerRadius(6.dp.toPx())
        )

        // ── Ghost grid lines ───────────────────────────────────────
        for (row in 0..rows) {
            drawLine(
                color = gridColor,
                start = Offset(0f, cellH * row),
                end = Offset(w, cellH * row),
                strokeWidth = strokeLine,
                cap = StrokeCap.Round
            )
        }
        for (col in 0..cols) {
            drawLine(
                color = gridColor,
                start = Offset(cellW * col, 0f),
                end = Offset(cellW * col, h),
                strokeWidth = strokeLine,
                cap = StrokeCap.Round
            )
        }

        // ── Active player lines (coral) ────────────────────────────
        // top edge of top-left box
        drawActiveLine(coral, Offset(0f, 0f), Offset(cellW, 0f), strokeActive)
        // bottom edge of top-left box
        drawActiveLine(coral, Offset(0f, cellH), Offset(cellW, cellH), strokeActive)
        // left edge of top-left box
        drawActiveLine(coral, Offset(0f, 0f), Offset(0f, cellH), strokeActive)
        // right edge of top-left box
        drawActiveLine(coral, Offset(cellW, 0f), Offset(cellW, cellH), strokeActive)

        // ── Active player lines (purple) for center box ────────────
        drawActiveLine(purple, Offset(cellW, cellH), Offset(cellW * 2, cellH), strokeActive)
        drawActiveLine(purple, Offset(cellW, cellH * 2), Offset(cellW * 2, cellH * 2), strokeActive)
        drawActiveLine(purple, Offset(cellW, cellH), Offset(cellW, cellH * 2), strokeActive)
        drawActiveLine(purple, Offset(cellW * 2, cellH), Offset(cellW * 2, cellH * 2), strokeActive)

        // ── A single "in-progress" gold line (bottom-right) ────────
        drawActiveLine(
            gold,
            Offset(cellW * 2, cellH * 2),
            Offset(cellW * 3, cellH * 2),
            strokeActive * 0.9f,
            dashOffset = true
        )

        // ── Dots ───────────────────────────────────────────────────
        for (row in 0..rows) {
            for (col in 0..cols) {
                val cx = cellW * col
                val cy = cellH * row
                // glow ring for "active" dots
                val isCornerOfActive =
                    (row == 0 && col == 0) || (row == 0 && col == 1) ||
                    (row == 1 && col == 0) || (row == 1 && col == 1)
                if (isCornerOfActive) {
                    drawCircle(
                        color = coral.copy(alpha = 0.20f),
                        radius = dotR * 2.4f,
                        center = Offset(cx, cy)
                    )
                }
                drawCircle(color = dotColor, radius = dotR, center = Offset(cx, cy))
                drawCircle(color = Color.White.copy(alpha = 0.7f), radius = dotR * 0.38f, center = Offset(cx, cy))
            }
        }
    }
}

private fun DrawScope.drawActiveLine(
    color: Color,
    start: Offset,
    end: Offset,
    stroke: Float,
    dashOffset: Boolean = false
) {
    if (dashOffset) {
        // draw dashed
        val total = (end - start).getDistance()
        val dash = 8.dp.toPx()
        val gap = 5.dp.toPx()
        val dir = (end - start) / total
        var pos = 0f
        while (pos < total) {
            val s = start + dir * pos
            val e = start + dir * (pos + dash).coerceAtMost(total)
            drawLine(color = color, start = s, end = e, strokeWidth = stroke, cap = StrokeCap.Round)
            pos += dash + gap
        }
    } else {
        drawLine(color = color, start = start, end = end, strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

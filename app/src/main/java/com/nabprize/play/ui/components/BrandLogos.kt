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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

// ─── Jazz Logo ───────────────────────────────────────────────────────────────
// Red circle, white bold "J" with a curve at bottom
@Composable
fun JazzLogo(size: Dp = 40.dp) {
    Canvas(Modifier.size(size)) {
        val r = this.size.minDimension / 2f
        val cx = r; val cy = r
        // Background circle
        drawCircle(color = Color(0xFFCC0000), radius = r, center = Offset(cx, cy))
        // J stroke
        val sw = r * 0.22f
        val topX = cx + r * 0.12f
        val topY = cy - r * 0.44f
        val bottomY = cy + r * 0.22f
        // vertical bar
        drawLine(
            color = Color.White, strokeWidth = sw, cap = StrokeCap.Round,
            start = Offset(topX, topY), end = Offset(topX, bottomY)
        )
        // curve hook left
        val path = Path().apply {
            moveTo(topX, bottomY)
            cubicTo(
                topX, cy + r * 0.50f,
                cx - r * 0.40f, cy + r * 0.52f,
                cx - r * 0.42f, cy + r * 0.28f
            )
        }
        drawPath(path, Color.White, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        // dot above J
        drawCircle(color = Color.White, radius = sw * 0.6f, center = Offset(topX, topY - sw * 0.8f))
    }
}

// ─── Telenor Logo ────────────────────────────────────────────────────────────
// Blue circle, stylised white "t" with a bolder crossbar
@Composable
fun TelenorLogo(size: Dp = 40.dp) {
    Canvas(Modifier.size(size)) {
        val r = this.size.minDimension / 2f
        val cx = r; val cy = r
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF0070BA), Color(0xFF005EA6)),
                start = Offset(0f, 0f), end = Offset(r * 2, r * 2)
            ),
            radius = r, center = Offset(cx, cy)
        )
        val sw = r * 0.20f
        // vertical stem
        drawLine(
            color = Color.White, strokeWidth = sw, cap = StrokeCap.Round,
            start = Offset(cx, cy - r * 0.46f), end = Offset(cx, cy + r * 0.46f)
        )
        // crossbar
        drawLine(
            color = Color.White, strokeWidth = sw * 0.85f, cap = StrokeCap.Round,
            start = Offset(cx - r * 0.36f, cy - r * 0.10f),
            end = Offset(cx + r * 0.36f, cy - r * 0.10f)
        )
    }
}

// ─── Zong Logo ───────────────────────────────────────────────────────────────
// Yellow-Green gradient circle, bold white "Z"
@Composable
fun ZongLogo(size: Dp = 40.dp) {
    Canvas(Modifier.size(size)) {
        val r = this.size.minDimension / 2f
        val cx = r; val cy = r
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF7FC31C), Color(0xFF5AA000)),
                start = Offset(0f, 0f), end = Offset(r * 2, r * 2)
            ),
            radius = r, center = Offset(cx, cy)
        )
        val sw = r * 0.19f
        val lp = r * 0.38f
        val tp = cy - r * 0.35f
        val bp = cy + r * 0.35f
        val path = Path().apply {
            moveTo(cx - lp, tp)
            lineTo(cx + lp, tp)
            lineTo(cx - lp, bp)
            lineTo(cx + lp, bp)
        }
        drawPath(path, Color.White, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

// ─── Ufone Logo ──────────────────────────────────────────────────────────────
// Orange circle, white bold "U"
@Composable
fun UfoneLogo(size: Dp = 40.dp) {
    Canvas(Modifier.size(size)) {
        val r = this.size.minDimension / 2f
        val cx = r; val cy = r
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFFF6600), Color(0xFFE05500)),
                start = Offset(0f, 0f), end = Offset(r * 2, r * 2)
            ),
            radius = r, center = Offset(cx, cy)
        )
        val sw = r * 0.20f
        val lx = cx - r * 0.30f
        val rx = cx + r * 0.30f
        val topY = cy - r * 0.42f
        val midY = cy + r * 0.12f
        val path = Path().apply {
            moveTo(lx, topY)
            lineTo(lx, midY)
            cubicTo(lx, cy + r * 0.50f, rx, cy + r * 0.50f, rx, midY)
            lineTo(rx, topY)
        }
        drawPath(path, Color.White, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

// ─── PUBG Mobile Logo ────────────────────────────────────────────────────────
// Dark background, gold frying-pan silhouette (iconic PUBG symbol)
@Composable
fun PubgLogo(size: Dp = 40.dp) {
    Canvas(Modifier.size(size)) {
        val r = this.size.minDimension / 2f
        val cx = r; val cy = r
        // Dark bg
        drawRoundRect(
            color = Color(0xFF1A1208),
            size = Size(r * 2, r * 2),
            cornerRadius = CornerRadius(r * 0.28f)
        )
        val gold = Color(0xFFFFB800)
        // Pan circle (head)
        val panR = r * 0.42f
        val panCy = cy - r * 0.08f
        drawCircle(color = gold, radius = panR, center = Offset(cx, panCy))
        // Pan handle
        val handleW = r * 0.17f
        val handlePath = Path().apply {
            moveTo(cx - handleW / 2, panCy + panR - 2)
            lineTo(cx - handleW / 2, cy + r * 0.50f)
            lineTo(cx + handleW / 2, cy + r * 0.50f)
            lineTo(cx + handleW / 2, panCy + panR - 2)
            close()
        }
        drawPath(handlePath, gold)
        // Inner circle (dark) to hollow pan
        drawCircle(color = Color(0xFF1A1208), radius = panR * 0.62f, center = Offset(cx, panCy))
        // helmet visor slit
        drawLine(
            color = gold, strokeWidth = r * 0.10f, cap = StrokeCap.Round,
            start = Offset(cx - panR * 0.40f, panCy + panR * 0.10f),
            end = Offset(cx + panR * 0.40f, panCy + panR * 0.10f)
        )
    }
}

// ─── Free Fire Logo ──────────────────────────────────────────────────────────
// Red-orange gradient, white diamond/flame shape
@Composable
fun FreeFireLogo(size: Dp = 40.dp) {
    Canvas(Modifier.size(size)) {
        val r = this.size.minDimension / 2f
        val cx = r; val cy = r
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFFF3A00), Color(0xFFCC0A00)),
                start = Offset(0f, 0f), end = Offset(r * 2, r * 2)
            ),
            size = Size(r * 2, r * 2),
            cornerRadius = CornerRadius(r * 0.28f)
        )
        // Outer diamond shape
        val diamondPath = Path().apply {
            moveTo(cx, cy - r * 0.50f)        // top
            lineTo(cx + r * 0.42f, cy)        // right
            lineTo(cx, cy + r * 0.50f)        // bottom
            lineTo(cx - r * 0.42f, cy)        // left
            close()
        }
        drawPath(diamondPath, Color.White)
        // Inner diamond cutout (FF effect)
        val inner = Path().apply {
            moveTo(cx, cy - r * 0.24f)
            lineTo(cx + r * 0.20f, cy)
            lineTo(cx, cy + r * 0.24f)
            lineTo(cx - r * 0.20f, cy)
            close()
        }
        drawPath(inner, Color(0xFFCC0A00))
        // flame tip above
        val flamePath = Path().apply {
            moveTo(cx - r * 0.12f, cy - r * 0.46f)
            cubicTo(cx - r * 0.06f, cy - r * 0.68f, cx + r * 0.16f, cy - r * 0.62f, cx, cy - r * 0.78f)
            cubicTo(cx + r * 0.10f, cy - r * 0.56f, cx + r * 0.22f, cy - r * 0.52f, cx + r * 0.12f, cy - r * 0.46f)
            close()
        }
        drawPath(flamePath, Color(0xFFFFC700))
    }
}

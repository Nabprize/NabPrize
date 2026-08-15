package com.nabprize.play.ui.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nabprize.play.ui.theme.AccentGold
import com.nabprize.play.ui.theme.PrimaryOrange
import com.nabprize.play.ui.theme.SecondaryPurple
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─── Data Model ──────────────────────────────────────────────────────────────

data class Line(val r1: Int, val c1: Int, val r2: Int, val c2: Int)

enum class Owner { NONE, PLAYER, BOT }

data class BoardState(
    val gridSize: Int = 5,
    val lines: Map<Line, Owner> = emptyMap(),
    val boxes: Map<Pair<Int, Int>, Owner> = emptyMap(),
    val playerScore: Int = 0,
    val botScore: Int = 0,
    val isPlayerTurn: Boolean = true,
    val isGameOver: Boolean = false
) {
    val totalCells get() = (gridSize - 1) * (gridSize - 1)
    val winner: Owner
        get() = when {
            !isGameOver -> Owner.NONE
            playerScore > botScore -> Owner.PLAYER
            botScore > playerScore -> Owner.BOT
            else -> Owner.NONE
        }
}

// ─── Board Logic ─────────────────────────────────────────────────────────────

internal fun Line.normalised() =
    if (r1 < r2 || (r1 == r2 && c1 < c2)) this else Line(r2, c2, r1, c1)

private fun completedBoxes(line: Line, lines: Map<Line, Owner>, gridSize: Int): List<Pair<Int, Int>> {
    val result = mutableListOf<Pair<Int, Int>>()
    val n = line.normalised()
    val isHorizontal = n.r1 == n.r2

    fun boxComplete(row: Int, col: Int): Boolean {
        if (row < 0 || row >= gridSize - 1 || col < 0 || col >= gridSize - 1) return false
        val top    = Line(row, col, row, col + 1).normalised()
        val bottom = Line(row + 1, col, row + 1, col + 1).normalised()
        val left   = Line(row, col, row + 1, col).normalised()
        val right  = Line(row, col + 1, row + 1, col + 1).normalised()
        return lines.containsKey(top) && lines.containsKey(bottom) &&
               lines.containsKey(left) && lines.containsKey(right)
    }

    if (isHorizontal) {
        if (boxComplete(n.r1 - 1, n.c1)) result.add(n.r1 - 1 to n.c1)
        if (boxComplete(n.r1, n.c1))     result.add(n.r1 to n.c1)
    } else {
        if (boxComplete(n.r1, n.c1 - 1)) result.add(n.r1 to n.c1 - 1)
        if (boxComplete(n.r1, n.c1))     result.add(n.r1 to n.c1)
    }
    return result
}

fun botMove(state: BoardState): Line {
    val g = state.gridSize
    val allLines = buildList {
        for (r in 0 until g) for (c in 0 until g - 1) add(Line(r, c, r, c + 1))
        for (r in 0 until g - 1) for (c in 0 until g) add(Line(r, c, r + 1, c))
    }.map { it.normalised() }.distinct()

    val available = allLines.filter { !state.lines.containsKey(it) }
    if (available.isEmpty()) return allLines.first()

    fun boxesCompleted(line: Line, lines: Map<Line, Owner>) =
        completedBoxes(line, lines, g).size

    fun opportunityCount(line: Line, lines: Map<Line, Owner>): Int {
        val newLines = lines + (line to Owner.BOT)
        var count = 0
        for (r in 0 until g - 1) for (c in 0 until g - 1) {
            if (state.boxes.containsKey(r to c)) continue
            val top    = Line(r, c, r, c + 1).normalised()
            val bottom = Line(r + 1, c, r + 1, c + 1).normalised()
            val left   = Line(r, c, r + 1, c).normalised()
            val right  = Line(r, c + 1, r + 1, c + 1).normalised()
            val sides = listOf(top, bottom, left, right).count { newLines.containsKey(it) }
            if (sides == 3) count++
        }
        return count
    }

    // 1. Complete boxes greedily
    val completingMoves = available
        .map { it to boxesCompleted(it, state.lines) }
        .filter { it.second > 0 }
        .sortedByDescending { it.second }
    if (completingMoves.isNotEmpty()) return completingMoves.first().first

    // 2. Safe moves
    val safeMoves = available
        .map { it to opportunityCount(it, state.lines) }
        .filter { it.second == 0 }
    if (safeMoves.isNotEmpty()) return safeMoves.random().first

    // 3. Forced sacrifice — fewest boxes opened
    return available
        .map { it to opportunityCount(it, state.lines) }
        .sortedBy { it.second }
        .first().first
}

fun applyMove(state: BoardState, line: Line, owner: Owner): BoardState {
    val nLine = line.normalised()
    if (state.lines.containsKey(nLine) || state.isGameOver) return state

    val newLines = state.lines + (nLine to owner)
    val claimed  = completedBoxes(nLine, newLines, state.gridSize)
    val newBoxes = state.boxes + claimed.associateWith { owner }
    val newPlayerScore = newBoxes.values.count { it == Owner.PLAYER }
    val newBotScore    = newBoxes.values.count { it == Owner.BOT }
    val totalLines = (state.gridSize - 1) * state.gridSize * 2
    val gameOver = newLines.size == totalLines

    val nextTurn = if (owner == Owner.PLAYER && claimed.isNotEmpty()) true
                   else if (owner == Owner.BOT && claimed.isNotEmpty()) false
                   else owner != Owner.PLAYER

    return state.copy(
        lines = newLines,
        boxes = newBoxes,
        playerScore = newPlayerScore,
        botScore = newBotScore,
        isPlayerTurn = nextTurn,
        isGameOver = gameOver
    )
}

// ─── Theme Colours ────────────────────────────────────────────────────────────

internal val PlayerColor = PrimaryOrange
internal val BotColor    = SecondaryPurple
internal val GoldColor   = AccentGold

// ─── Floating Score Label Model ───────────────────────────────────────────────

private data class FloatingLabel(
    val id: Int,
    val text: String,
    val color: Color,
    val centerX: Float,   // px
    val centerY: Float,   // px
)

// ─── Snap Helpers ─────────────────────────────────────────────────────────────

internal fun snapToDot(
    offset: Offset, cellW: Float, cellH: Float, gridSize: Int, snapThreshold: Float
): Pair<Int, Int>? {
    var best: Pair<Int, Int>? = null
    var bestDist = Float.MAX_VALUE
    for (r in 0 until gridSize) for (c in 0 until gridSize) {
        val dx = cellW * c - offset.x
        val dy = cellH * r - offset.y
        val d = kotlin.math.sqrt(dx * dx + dy * dy)
        if (d < snapThreshold && d < bestDist) { bestDist = d; best = r to c }
    }
    return best
}

internal fun findTargetDot(
    startRow: Int, startCol: Int, pos: Offset,
    cellW: Float, cellH: Float, gridSize: Int, snapThreshold: Float
): Pair<Int, Int>? {
    val candidates = listOf(
        startRow to startCol - 1,
        startRow to startCol + 1,
        startRow - 1 to startCol,
        startRow + 1 to startCol
    ).filter { (r, c) -> r in 0 until gridSize && c in 0 until gridSize }

    var best: Pair<Int, Int>? = null
    var bestDist = Float.MAX_VALUE
    for ((r, c) in candidates) {
        val dx = cellW * c - pos.x
        val dy = cellH * r - pos.y
        val d = kotlin.math.sqrt(dx * dx + dy * dy)
        if (d < snapThreshold && d < bestDist) { bestDist = d; best = r to c }
    }
    return best
}

// ─── Main Board Composable ────────────────────────────────────────────────────

@Composable
fun DotsAndBoxesBoard(
    state: BoardState,
    onLineClick: (Line) -> Unit,
    modifier: Modifier = Modifier
) {
    val lineProgress = remember { mutableStateMapOf<Line, Animatable<Float, androidx.compose.animation.core.AnimationVector1D>>() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    // Box scale pop per cell
    val boxScales = remember { mutableStateMapOf<Pair<Int,Int>, Animatable<Float, androidx.compose.animation.core.AnimationVector1D>>() }

    // Floating "+1" labels
    val floatingLabels = remember { mutableStateListOf<FloatingLabel>() }
    var floatingIdCounter by remember { mutableStateOf(0) }

    // Haptics on box claim + launch float label + scale pop
    var previousBoxes by remember { mutableStateOf(state.boxes) }
    LaunchedEffect(state.boxes) {
        val newEntries = state.boxes.filter { !previousBoxes.containsKey(it.key) }
        if (newEntries.isNotEmpty()) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        newEntries.forEach { (pos, owner) ->
            // Scale pop animation
            val anim = Animatable(0f)
            boxScales[pos] = anim
            scope.launch {
                anim.animateTo(1.15f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium))
                anim.animateTo(1f, tween(120))
            }
        }
        previousBoxes = state.boxes
    }

    // Line draw animation
    LaunchedEffect(state.lines.size) {
        state.lines.keys.forEach { ln ->
            if (!lineProgress.containsKey(ln)) {
                val anim = Animatable(0f)
                lineProgress[ln] = anim
                scope.launch { anim.animateTo(1f, tween(280, easing = FastOutSlowInEasing)) }
            }
        }
    }

    // Infinite pulse for the "active" border ring
    val infiniteTransition = rememberInfiniteTransition(label = "board_pulse")
    val borderAlpha by infiniteTransition.animateFloat(
        0.55f, 1.0f,
        infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "border_alpha"
    )
    val borderColor by infiniteTransition.animateColor(
        initialValue = if (state.isPlayerTurn) PlayerColor else BotColor,
        targetValue  = if (state.isPlayerTurn) PlayerColor.copy(0.6f) else BotColor.copy(0.6f),
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "border_color"
    )

    // Drag state
    var dragStartDot  by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var dragEndDot    by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var dragFingerPos by remember { mutableStateOf<Offset?>(null) }
    var isDragging    by remember { mutableStateOf(false) }

    // Board pixel size (captured during first layout)
    var boardPxSize by remember { mutableStateOf(0f) }

    Box(modifier = modifier.fillMaxWidth().aspectRatio(1f)) {

        // ── Layer 1: Main board canvas ──────────────────────────────
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(null) { boardPxSize = size.width.toFloat() }
        ) {
            val w = size.width
            val h = size.height
            boardPxSize = w
            val g = state.gridSize
            val cellW = w / (g - 1)
            val cellH = h / (g - 1)
            val strokeW = 6.dp.toPx()
            val ghostW  = 2.5f.dp.toPx()
            val dotR    = 8.dp.toPx()

            // ── Board background with subtle cross-hatch grid ─────
            drawRoundRect(
                brush = Brush.linearGradient(
                    listOf(Color(0xFFF8F2E8), Color(0xFFF1E8D8)),
                    start = Offset(0f, 0f), end = Offset(w, h)
                ),
                topLeft = Offset.Zero, size = Size(w, h),
                cornerRadius = CornerRadius(16.dp.toPx())
            )
            // Subtle grid crosshair marks in background
            val gridAlpha = 0.07f
            for (r in 0..g - 1) for (c in 0..g - 1) {
                val cx = cellW * c; val cy = cellH * r
                drawLine(Color(0xFF8B6914).copy(alpha = gridAlpha), Offset(cx - 5, cy), Offset(cx + 5, cy), 1.dp.toPx())
                drawLine(Color(0xFF8B6914).copy(alpha = gridAlpha), Offset(cx, cy - 5), Offset(cx, cy + 5), 1.dp.toPx())
            }

            // ── Active border ring (animated, changes with turn) ──
            if (!state.isGameOver) {
                drawRoundRect(
                    color = borderColor.copy(alpha = borderAlpha * 0.6f),
                    topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                    size = Size(w - 4.dp.toPx(), h - 4.dp.toPx()),
                    cornerRadius = CornerRadius(15.dp.toPx()),
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            // ── Box fills (with animated scale) ──────────────────
            state.boxes.forEach { (pos, owner) ->
                val (row, col) = pos
                val color = if (owner == Owner.PLAYER) PlayerColor else BotColor
                val scale = boxScales[pos]?.value ?: 1f
                val cx = cellW * (col + 0.5f); val cy = cellH * (row + 0.5f)
                val hw = (cellW * scale) / 2f; val hh = (cellH * scale) / 2f

                drawRoundRect(
                    brush = Brush.linearGradient(
                        listOf(color.copy(alpha = 0.45f), color.copy(alpha = 0.20f)),
                        start = Offset(cx - hw, cy - hh), end = Offset(cx + hw, cy + hh)
                    ),
                    topLeft = Offset(cx - hw + 2, cy - hh + 2),
                    size = Size(hw * 2 - 4, hh * 2 - 4),
                    cornerRadius = CornerRadius(10.dp.toPx())
                )
                // Radial center glow
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(color.copy(alpha = 0.25f), Color.Transparent),
                        center = Offset(cx, cy), radius = cellW * 0.45f
                    ),
                    radius = cellW * 0.45f, center = Offset(cx, cy)
                )
            }

            // ── Ghost (available) lines ───────────────────────────
            for (r in 0 until g) for (c in 0 until g - 1) {
                val ln = Line(r, c, r, c + 1).normalised()
                if (!state.lines.containsKey(ln)) {
                    drawLine(
                        Color(0xFFC4B8A4).copy(alpha = 0.55f),
                        Offset(cellW * c + dotR * 0.9f, cellH * r),
                        Offset(cellW * (c + 1) - dotR * 0.9f, cellH * r),
                        ghostW, cap = StrokeCap.Round
                    )
                }
            }
            for (r in 0 until g - 1) for (c in 0 until g) {
                val ln = Line(r, c, r + 1, c).normalised()
                if (!state.lines.containsKey(ln)) {
                    drawLine(
                        Color(0xFFC4B8A4).copy(alpha = 0.55f),
                        Offset(cellW * c, cellH * r + dotR * 0.9f),
                        Offset(cellW * c, cellH * (r + 1) - dotR * 0.9f),
                        ghostW, cap = StrokeCap.Round
                    )
                }
            }

            // ── Drawn lines (animated draw-in) ────────────────────
            state.lines.forEach { (ln, owner) ->
                val color = if (owner == Owner.PLAYER) PlayerColor else BotColor
                val prog = lineProgress[ln]?.value ?: 1f
                val sx = cellW * ln.c1; val sy = cellH * ln.r1
                val ex = cellW * ln.c2; val ey = cellH * ln.r2
                val curEx = sx + (ex - sx) * prog
                val curEy = sy + (ey - sy) * prog

                // Outer glow
                drawLine(color.copy(alpha = 0.22f * prog), Offset(sx, sy), Offset(curEx, curEy), strokeW * 3f, cap = StrokeCap.Round)
                // Main gradient line
                drawLine(
                    Brush.linearGradient(
                        listOf(color, color.copy(alpha = 0.85f)),
                        Offset(sx, sy), Offset(curEx, curEy)
                    ),
                    Offset(sx, sy), Offset(curEx, curEy), strokeW, cap = StrokeCap.Round
                )
                // Specular highlight
                drawLine(Color.White.copy(alpha = 0.40f * prog), Offset(sx, sy), Offset(curEx, curEy), strokeW * 0.38f, cap = StrokeCap.Round)
            }

            // ── Dots ──────────────────────────────────────────────
            for (r in 0 until g) for (c in 0 until g) {
                val cx = cellW * c; val cy = cellH * r

                // Outer soft halo
                drawCircle(
                    Brush.radialGradient(
                        listOf(Color(0xFF8B6914).copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(cx, cy), radius = dotR * 3f
                    ), dotR * 3f, Offset(cx, cy)
                )
                // Ring
                drawCircle(Color(0xFFC4B8A4).copy(alpha = 0.4f), dotR * 1.55f, Offset(cx, cy))
                // Body — dark
                drawCircle(Color(0xFF2C2825), dotR, Offset(cx, cy))
                // Inner gloss
                drawCircle(Color.White.copy(alpha = 0.55f), dotR * 0.30f, Offset(cx - dotR * 0.22f, cy - dotR * 0.22f))
            }
        }

        // ── Layer 2: Owner initials / emoji in filled boxes ───────
        if (boardPxSize > 0f) {
            val g = state.gridSize
            val cellW = boardPxSize / (g - 1)
            val cellH = boardPxSize / (g - 1)

            state.boxes.forEach { (pos, owner) ->
                val (row, col) = pos
                val cx = cellW * (col + 0.5f)
                val cy = cellH * (row + 0.5f)
                val label = if (owner == Owner.PLAYER) "★" else "🤖"
                val scale  = boxScales[pos]?.value ?: 1f
                val color  = if (owner == Owner.PLAYER) PlayerColor else BotColor

                with(density) {
                    val xPx = cx - 8.dp.toPx()
                    val yPx = cy - 8.dp.toPx()
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(xPx.toInt(), yPx.toInt()) }
                    ) {
                        Text(
                            text = label,
                            fontSize = (14 * scale).sp,
                            fontWeight = FontWeight.Bold,
                            color = color.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // ── Layer 3: Drag preview overlay ─────────────────────────
        if (isDragging && dragStartDot != null && dragFingerPos != null) {
            DragPreviewOverlay(
                state       = state,
                startDot    = dragStartDot!!,
                fingerPos   = dragFingerPos!!,
                targetDot   = dragEndDot
            )
        }

        // ── Layer 4: Gesture capture (transparent, on top) ────────
        Box(
            Modifier
                .fillMaxSize()
                // Tap-to-draw
                .pointerInput(state) {
                    detectTapGestures { offset ->
                        val w = size.width.toFloat()
                        val g = state.gridSize
                        val cellW = w / (g - 1)
                        val cellH = w / (g - 1)
                        val snapDist = cellW * 0.38f

                        var bestLine: Line? = null
                        var bestDist = Float.MAX_VALUE

                        for (r in 0 until g) for (c in 0 until g - 1) {
                            val sx = cellW * c; val sy = cellH * r
                            val mx = (sx + cellW * (c + 1)) / 2; val my = sy
                            val d = kotlin.math.sqrt((offset.x - mx).let { it * it } + (offset.y - my).let { it * it })
                            if (d < snapDist && d < bestDist) { bestDist = d; bestLine = Line(r, c, r, c + 1) }
                        }
                        for (r in 0 until g - 1) for (c in 0 until g) {
                            val sx = cellW * c; val sy = cellH * r
                            val mx = sx; val my = (sy + cellH * (r + 1)) / 2
                            val d = kotlin.math.sqrt((offset.x - mx).let { it * it } + (offset.y - my).let { it * it })
                            if (d < snapDist && d < bestDist) { bestDist = d; bestLine = Line(r, c, r + 1, c) }
                        }
                        bestLine?.let {
                            if (!state.lines.containsKey(it.normalised())) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onLineClick(it)
                            }
                        }
                    }
                }
                // Drag-to-draw
                .pointerInput(state) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val w = size.width.toFloat()
                            val g = state.gridSize
                            val cellW = w / (g - 1); val cellH = w / (g - 1)
                            val dot = snapToDot(offset, cellW, cellH, g, cellW * 0.50f)
                            dragStartDot  = dot
                            dragEndDot    = null
                            dragFingerPos = offset
                            isDragging    = dot != null
                        },
                        onDrag = { change, _ ->
                            val w = size.width.toFloat()
                            val g = state.gridSize
                            val cellW = w / (g - 1); val cellH = w / (g - 1)
                            val start = dragStartDot ?: return@detectDragGestures
                            dragFingerPos = change.position
                            val target = findTargetDot(start.first, start.second, change.position, cellW, cellH, g, cellW * 0.65f)
                            if (target != dragEndDot && target != null)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            dragEndDot = target
                        },
                        onDragEnd = {
                            val start = dragStartDot; val end = dragEndDot
                            if (start != null && end != null) {
                                val line = Line(start.first, start.second, end.first, end.second).normalised()
                                if (!state.lines.containsKey(line)) onLineClick(line)
                            }
                            dragStartDot = null; dragEndDot = null; dragFingerPos = null; isDragging = false
                        },
                        onDragCancel = {
                            dragStartDot = null; dragEndDot = null; dragFingerPos = null; isDragging = false
                        }
                    )
                }
        )
    }
}

// ─── Drag Preview Overlay ─────────────────────────────────────────────────────

@Composable
private fun DragPreviewOverlay(
    state: BoardState,
    startDot: Pair<Int, Int>,
    fingerPos: Offset,
    targetDot: Pair<Int, Int>?
) {
    val infinite = rememberInfiniteTransition(label = "drag_pulse")
    val pulse by infinite.animateFloat(0.72f, 1.0f, infiniteRepeatable(tween(550, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
    val pulseGlow by infinite.animateFloat(0.18f, 0.45f, infiniteRepeatable(tween(750, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow")

    val hasSnap = targetDot != null

    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val g = state.gridSize
        val cellW = w / (g - 1); val cellH = w / (g - 1)
        val thickLine = 13.dp.toPx()
        val dotR = 9.dp.toPx()

        val sx = cellW * startDot.second; val sy = cellH * startDot.first
        val endX = if (hasSnap) cellW * targetDot!!.second else fingerPos.x
        val endY = if (hasSnap) cellH * targetDot!!.first  else fingerPos.y

        // Valid adjacent dot indicators
        val candidates = listOf(
            startDot.first to startDot.second - 1,
            startDot.first to startDot.second + 1,
            startDot.first - 1 to startDot.second,
            startDot.first + 1 to startDot.second
        ).filter { (r, c) ->
            r in 0 until g && c in 0 until g &&
            !state.lines.containsKey(Line(startDot.first, startDot.second, r, c).normalised())
        }

        for ((r, c) in candidates) {
            val cx = cellW * c; val cy = cellH * r
            val isTarget = hasSnap && targetDot!!.first == r && targetDot.second == c
            val ring = if (isTarget) dotR * 3.2f * pulse else dotR * 2.4f
            val rAlpha = if (isTarget) 1.0f else 0.5f * pulse
            val strokePx = if (isTarget) 4.5f.dp.toPx() else 2.5f.dp.toPx()

            val segs = 10; val gap = 360f / segs / 2.2f
            for (i in 0 until segs) {
                drawArc(
                    color = if (isTarget) PlayerColor else PlayerColor.copy(alpha = rAlpha),
                    startAngle = i * (360f / segs),
                    sweepAngle = (360f / segs) - gap,
                    useCenter = false,
                    topLeft = Offset(cx - ring, cy - ring),
                    size = Size(ring * 2, ring * 2),
                    style = Stroke(strokePx, cap = StrokeCap.Round)
                )
            }
            if (isTarget) {
                drawCircle(
                    Brush.radialGradient(listOf(PlayerColor.copy(pulseGlow * 1.8f), Color.Transparent), Offset(cx, cy), dotR * 7f),
                    dotR * 7f, Offset(cx, cy)
                )
                drawCircle(PlayerColor.copy(0.8f * pulse), dotR * 1.3f, Offset(cx, cy))
            }
        }

        // Drag line
        val gw = if (hasSnap) thickLine * 4.8f else thickLine * 3.6f
        drawLine(PlayerColor.copy(if (hasSnap) 0.38f else 0.22f), Offset(sx, sy), Offset(endX, endY), gw, cap = StrokeCap.Round)
        drawLine(
            Brush.linearGradient(
                listOf(PlayerColor, if (hasSnap) PlayerColor.copy(0.95f) else PlayerColor.copy(0.72f)),
                Offset(sx, sy), Offset(endX, endY)
            ),
            Offset(sx, sy), Offset(endX, endY), thickLine, cap = StrokeCap.Round
        )
        drawLine(Color.White.copy(if (hasSnap) 0.62f else 0.40f), Offset(sx, sy), Offset(endX, endY), thickLine * 0.50f, cap = StrokeCap.Round)

        // End glow
        val egr = if (hasSnap) dotR * 6.5f else dotR * 5f
        drawCircle(Brush.radialGradient(listOf(PlayerColor.copy(if (hasSnap) 0.55f else 0.30f), Color.Transparent), Offset(endX, endY), egr), egr, Offset(endX, endY))
        // Start glow
        drawCircle(Brush.radialGradient(listOf(PlayerColor.copy(0.55f), Color.Transparent), Offset(sx, sy), dotR * 4.5f), dotR * 4.5f, Offset(sx, sy))
    }
}

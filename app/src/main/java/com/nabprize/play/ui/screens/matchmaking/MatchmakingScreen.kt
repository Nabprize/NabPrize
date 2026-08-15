package com.nabprize.play.ui.screens.matchmaking

import android.os.Handler
import android.os.Looper
import com.nabprize.play.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nabprize.play.ui.ads.AdMobBanner
import com.nabprize.play.ui.ads.AdMobNativeAd
import com.nabprize.play.ui.components.BoardState
import com.nabprize.play.ui.components.DotsAndBoxesBoard
import com.nabprize.play.ui.components.Line
import com.nabprize.play.ui.components.NabPrizeButton
import com.nabprize.play.ui.components.Owner
import com.nabprize.play.ui.theme.AccentGold
import com.nabprize.play.ui.theme.CardWhite
import com.nabprize.play.ui.theme.CreamBackground
import com.nabprize.play.ui.theme.PrimaryOrange
import com.nabprize.play.ui.theme.SecondaryPurple
import com.nabprize.play.ui.theme.StatGreen
import com.nabprize.play.ui.theme.TextPrimary
import com.nabprize.play.ui.theme.TextSecondary
import com.nabprize.play.ui.theme.TextTertiary

private enum class MatchPhase { SEARCHING, MATCH_FOUND, PLAYING, FINISHED, ERROR }

private data class Opponent(
    val id: String,
    val name: String,
    val isBot: Boolean,
    val label: String
)

private data class FinishedMatch(
    val won: Boolean,
    val playerBoxes: Int,
    val opponentBoxes: Int,
    val reason: String
)

private data class MatchUiState(
    val phase: MatchPhase = MatchPhase.SEARCHING,
    val matchId: String? = null,
    val opponent: Opponent? = null,
    val board: BoardState = BoardState(),
    val turnDeadline: Long = 0L,
    val secondsSearching: Int = 0,
    val readySent: Boolean = false,
    val error: String? = null,
    val result: FinishedMatch? = null
)

private sealed interface MatchEvent {
    data object Connected : MatchEvent
    data object QueueJoined : MatchEvent
    data class Found(val matchId: String, val opponent: Opponent) : MatchEvent
    data class State(val payload: JSONObject) : MatchEvent
    data class Finished(val payload: JSONObject) : MatchEvent
    data class Error(val message: String) : MatchEvent
}

/** Thin Socket.IO wrapper. All callbacks are posted to the main thread for Compose safety. */
private class MatchSocketClient(private val userId: String, private val displayName: String) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var socket: Socket? = null
    var onEvent: ((MatchEvent) -> Unit)? = null

    fun connect() {
        try {
            val options = IO.Options().apply {
                forceNew = true
                reconnection = true
                timeout = 5_000
                auth = hashMapOf("userId" to userId, "displayName" to displayName)
            }
            socket = IO.socket(BuildConfig.MATCH_SERVER_URL, options)
            val connectedSocket = socket ?: return
            connectedSocket
                .on(Socket.EVENT_CONNECT) { deliver(MatchEvent.Connected) }
                .on("queue_joined") { deliver(MatchEvent.QueueJoined) }
                .on("match_found") { args ->
                    val json = args.firstOrNull() as? JSONObject ?: return@on
                    val opponentJson = json.optJSONObject("opponent") ?: JSONObject()
                    deliver(
                        MatchEvent.Found(
                            matchId = json.optString("matchId"),
                            opponent = Opponent(
                                id = opponentJson.optString("userId", "bot"),
                                name = opponentDisplayName(opponentJson),
                                isBot = opponentJson.optBoolean("isBot", false),
                                label = opponentJson.optString("label", "Opponent")
                            )
                        )
                    )
                }
                .on("game_state") { args ->
                    (args.firstOrNull() as? JSONObject)?.let { deliver(MatchEvent.State(it)) }
                }
                .on("match_finished") { args ->
                    (args.firstOrNull() as? JSONObject)?.let { deliver(MatchEvent.Finished(it)) }
                }
                .on("match_error") { args ->
                    val json = args.firstOrNull() as? JSONObject
                    deliver(MatchEvent.Error(json?.optString("code", "Move rejected") ?: "Move rejected"))
                }
                .on(Socket.EVENT_CONNECT_ERROR) { deliver(MatchEvent.Error("Match server se connection nahi ho saka")) }
            socket?.connect()
        } catch (_: Exception) {
            deliver(MatchEvent.Error("Match server unavailable hai"))
        }
    }

    fun joinQueue() { socket?.emit("join_queue") }

    fun cancelQueue() { socket?.emit("cancel_queue") }

    fun ready(matchId: String) { socket?.emit("match_ready", JSONObject().put("matchId", matchId)) }

    fun move(matchId: String, line: Line) {
        val lineJson = JSONObject()
            .put("r1", line.r1).put("c1", line.c1)
            .put("r2", line.r2).put("c2", line.c2)
        socket?.emit("move", JSONObject().put("matchId", matchId).put("line", lineJson))
    }

    fun leave(matchId: String) { socket?.emit("leave_match", JSONObject().put("matchId", matchId)) }

    fun close() {
        socket?.off()
        socket?.disconnect()
        socket?.close()
        socket = null
    }

    private fun deliver(event: MatchEvent) {
        mainHandler.post { onEvent?.invoke(event) }
    }
}

@Composable
fun MatchmakingScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    playerName: String = "Player",
    onMatchResult: (Boolean) -> Unit = {}
) {
    val userId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "guest_${System.currentTimeMillis()}" }
    val authName = FirebaseAuth.getInstance().currentUser?.displayName?.trim().orEmpty()
    val emailName = FirebaseAuth.getInstance().currentUser?.email?.substringBefore("@")?.trim().orEmpty()
    val safePlayerName = playerName
        .takeUnless { it.isBlank() || it.equals("Player", ignoreCase = true) }
        ?: authName.takeIf { it.isNotBlank() }
        ?: emailName.takeIf { it.isNotBlank() }
        ?: "Player"
    val client = remember(userId, safePlayerName) { MatchSocketClient(userId, safePlayerName) }
    var ui by remember { mutableStateOf(MatchUiState()) }
    var resultRecorded by remember { mutableStateOf(false) }

    LaunchedEffect(client) {
        client.onEvent = { event ->
            ui = when (event) {
                MatchEvent.Connected -> ui
                MatchEvent.QueueJoined -> ui.copy(error = null)
                is MatchEvent.Found -> ui.copy(
                    phase = MatchPhase.MATCH_FOUND,
                    matchId = event.matchId,
                    opponent = event.opponent,
                    error = null
                )
                is MatchEvent.State -> {
                    val serverState = event.payload.optJSONObject("state")
                    val opponentId = serverState
                        ?.optJSONArray("players")?.let { players ->
                            (0 until players.length()).map { players.optString(it) }.firstOrNull { it != userId }
                        } ?: ui.opponent?.id ?: "bot"
                    ui.copy(
                        phase = if (serverState?.optString("status") == "ACTIVE") MatchPhase.PLAYING else MatchPhase.MATCH_FOUND,
                        matchId = event.payload.optString("matchId", ui.matchId ?: ""),
                        turnDeadline = event.payload.optLong("turnDeadline", 0L),
                        board = parseBoard(event.payload, userId, opponentId)
                    )
                }
                is MatchEvent.Finished -> {
                    val scores = event.payload.optJSONObject("scores") ?: JSONObject()
                    val playerBoxes = scores.optInt(userId, 0)
                    val opponentBoxes = ui.opponent?.id?.let { scores.optInt(it, 0) } ?: 0
                    val winner = event.payload.optString("winner", "")
                    val result = FinishedMatch(
                        won = winner == userId,
                        playerBoxes = playerBoxes,
                        opponentBoxes = opponentBoxes,
                        reason = event.payload.optString("reason", "completed")
                    )
                    if (!resultRecorded) {
                        resultRecorded = true
                        onMatchResult(result.won)
                    }
                    ui.copy(phase = MatchPhase.FINISHED, result = result)
                }
                is MatchEvent.Error -> ui.copy(phase = MatchPhase.ERROR, error = event.message)
            }
        }
        client.connect()
        client.joinQueue()
    }

    DisposableEffect(client) {
        onDispose { client.close() }
    }

    LaunchedEffect(ui.phase) {
        if (ui.phase == MatchPhase.SEARCHING) {
            while (true) {
                kotlinx.coroutines.delay(1_000)
                ui = ui.copy(secondsSearching = ui.secondsSearching + 1)
            }
        }
    }

    when (ui.phase) {
        MatchPhase.SEARCHING -> SearchingContent(
            modifier = modifier,
            seconds = ui.secondsSearching,
            error = ui.error,
            onBack = { client.cancelQueue(); onBack() },
            onRetry = { client.close(); ui = MatchUiState(); client.connect(); client.joinQueue() }
        )
        MatchPhase.MATCH_FOUND -> MatchFoundContent(
            modifier = modifier,
            playerName = safePlayerName,
            opponent = ui.opponent ?: Opponent("bot", "Ahsan Khan", true, "Auto-matched opponent"),
            ready = ui.readySent,
            onStart = {
                ui.matchId?.let(client::ready)
                ui = ui.copy(readySent = true)
            },
            onBack = { ui.matchId?.let(client::leave); onBack() }
        )
        MatchPhase.PLAYING -> OnlineGameContent(
            modifier = modifier,
            playerName = safePlayerName,
            opponent = ui.opponent ?: Opponent("bot", "Ahsan Khan", true, "Auto-matched opponent"),
            state = ui.board,
            turnDeadline = ui.turnDeadline,
            onMove = { line -> ui.matchId?.let { client.move(it, line) } },
            onBack = { ui.matchId?.let(client::leave); onBack() }
        )
        MatchPhase.FINISHED -> FinishedContent(
            modifier = modifier,
            playerName = safePlayerName,
            opponent = ui.opponent ?: Opponent("bot", "Ahsan Khan", true, "Auto-matched opponent"),
            result = ui.result ?: FinishedMatch(false, 0, 0, "completed"),
            onBack = onBack
        )
        MatchPhase.ERROR -> ErrorContent(
            modifier = modifier,
            message = ui.error ?: "Match server unavailable hai",
            onRetry = { client.close(); ui = MatchUiState(); client.connect(); client.joinQueue() },
            onBack = onBack
        )
    }
}

@Composable
private fun SearchingContent(
    modifier: Modifier,
    seconds: Int,
    error: String?,
    onBack: () -> Unit,
    onRetry: () -> Unit
) {
    MatchPage(modifier) {
        Header("Finding a Challenge", onBack)
        Spacer(Modifier.height(28.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
                .background(Brush.linearGradient(listOf(PrimaryOrange, AccentGold)))
                .padding(30.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(74.dp).clip(CircleShape).background(Color.White.copy(.18f)), contentAlignment = Alignment.Center) {
                    Text("VS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp)
                }
                Spacer(Modifier.height(18.dp))
                Text("Finding opponent...", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(8.dp))
                Text(
                    if (seconds < 20) "Searching for a nearby player" else "Almost ready — preparing your challenge",
                    color = Color.White.copy(.78f), textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Text("${seconds}s", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(22.dp))
        AdMobNativeAd(Modifier.fillMaxWidth())
        Spacer(Modifier.height(20.dp))
        if (error != null) {
            Text(error, color = Color(0xFFB3261E), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            NabPrizeButton("Try again", onRetry, Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
        }
        NabPrizeButton("Cancel search", onBack, Modifier.fillMaxWidth(), backgroundColor = Color(0xFFF3F4F6), contentColor = TextPrimary)
    }
}

@Composable
private fun MatchFoundContent(
    modifier: Modifier,
    playerName: String,
    opponent: Opponent,
    ready: Boolean,
    onStart: () -> Unit,
    onBack: () -> Unit
) {
    MatchPage(modifier) {
        Header("Challenge ready", onBack)
        Spacer(Modifier.height(32.dp))
        Text("Opponent found!", color = StatGreen, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(8.dp))
        Text(
            if (ready) "Waiting for the other player to ready up..." else "Both players are ready? Start when you are!",
            color = TextSecondary, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        OpponentCard(playerName = playerName, opponent = opponent)
        Spacer(Modifier.height(26.dp))
        NabPrizeButton(if (ready) "Waiting..." else "Start Challenge", onStart, Modifier.fillMaxWidth(), enabled = !ready)
    }
}

@Composable
private fun OnlineGameContent(
    modifier: Modifier,
    playerName: String,
    opponent: Opponent,
    state: BoardState,
    turnDeadline: Long,
    onMove: (Line) -> Unit,
    onBack: () -> Unit
) {
    var clockNow by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val turnLengthMs = 15_000L
    LaunchedEffect(state.isPlayerTurn, turnDeadline, state.isGameOver) {
        while (!state.isGameOver && turnDeadline > 0L) {
            clockNow = System.currentTimeMillis()
            kotlinx.coroutines.delay(100L)
        }
    }
    val remainingMs = (turnDeadline - clockNow).coerceIn(0L, turnLengthMs)
    val turnProgress = if (turnDeadline > 0L) remainingMs.toFloat() / turnLengthMs else 1f
    val remainingSeconds = ((remainingMs + 999L) / 1_000L).toInt().coerceIn(0, 15)

    MatchPage(modifier) {
        Header("Challenge", onBack)
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            ScorePill(playerName, state.playerScore, PrimaryOrange)
            Text("VS", color = TextTertiary, fontWeight = FontWeight.Black)
            ScorePill(opponent.name, state.botScore, SecondaryPurple)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            if (state.isGameOver) "Match complete" else if (state.isPlayerTurn) "Your turn — choose a line" else "Opponent is thinking...",
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
            color = if (state.isPlayerTurn) PrimaryOrange else TextSecondary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier.fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFFE8E2D8))
        ) {
            Box(
                Modifier.fillMaxWidth(turnProgress.coerceIn(0f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (turnProgress < 0.25f) Color(0xFFE05252) else PrimaryOrange)
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(
            if (state.isGameOver) "Match complete" else "$remainingSeconds sec remaining",
            color = if (turnProgress < 0.25f) Color(0xFFE05252) else TextTertiary,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End
        )
        Spacer(Modifier.height(10.dp))
        DotsAndBoxesBoard(
            state = state,
            onLineClick = { if (state.isPlayerTurn && !state.isGameOver) onMove(it) },
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(horizontal = 8.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text("15 seconds per turn • auto-line on timeout • 3 timeouts forfeit", color = TextTertiary, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(14.dp))
        AdMobBanner(Modifier.fillMaxWidth())
    }
}

@Composable
private fun FinishedContent(modifier: Modifier, playerName: String, opponent: Opponent, result: FinishedMatch, onBack: () -> Unit) {
    MatchPage(modifier) {
        Header("Challenge result", onBack)
        Spacer(Modifier.height(28.dp))
        Text(if (result.won) "Victory!" else "Good game!", color = if (result.won) StatGreen else SecondaryPurple, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(8.dp))
        Text("${result.playerBoxes} boxes for you • ${result.opponentBoxes} for ${opponent.name}", color = TextSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        OpponentCard(playerName, opponent, playerBoxes = result.playerBoxes, opponentBoxes = result.opponentBoxes)
        Spacer(Modifier.height(20.dp))
        Text(if (result.won) "+40 NP-Coins" else "+2 NP-Coins", color = AccentGold, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(26.dp))
        NabPrizeButton("Back to Home", onBack, Modifier.fillMaxWidth(), backgroundColor = Color(0xFFF3F4F6), contentColor = TextPrimary)
    }
}

@Composable
private fun ErrorContent(modifier: Modifier, message: String, onRetry: () -> Unit, onBack: () -> Unit) {
    MatchPage(modifier) {
        Header("Challenge", onBack)
        Spacer(Modifier.height(40.dp))
        Text("Connection problem", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text(message, color = TextSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(26.dp))
        NabPrizeButton("Retry", onRetry, Modifier.fillMaxWidth())
    }
}

@Composable
private fun MatchPage(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().background(CreamBackground).verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@Composable
private fun Header(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary) }
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold, color = TextPrimary))
    }
}

@Composable
private fun OpponentCard(playerName: String, opponent: Opponent, playerBoxes: Int? = null, opponentBoxes: Int? = null) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(CardWhite).padding(22.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarColumn(playerName, Icons.Default.Person, PrimaryOrange, playerBoxes)
        Text("VS", color = TextTertiary, fontWeight = FontWeight.Black)
        AvatarColumn(opponent.name, if (opponent.isBot) Icons.Default.SmartToy else Icons.Default.Person, SecondaryPurple, opponentBoxes, opponent.label)
    }
}

@Composable
private fun AvatarColumn(name: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, score: Int?, label: String? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(112.dp)) {
        Box(Modifier.size(62.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
        Spacer(Modifier.height(8.dp))
        Text(name, color = TextPrimary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1)
        Text(label ?: (score?.let { "$it boxes" } ?: "Player"), color = TextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ScorePill(name: String, score: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(110.dp)) {
        Text(name, color = color, fontWeight = FontWeight.Bold, maxLines = 1)
        Text("$score boxes", color = TextPrimary, fontSize = 13.sp)
    }
}

private fun parseBoard(payload: JSONObject, userId: String, opponentId: String): BoardState {
    val state = payload.optJSONObject("state") ?: return BoardState()
    val gridSize = state.optInt("gridSize", 5)
    val lines = linkedMapOf<Line, Owner>()
    val lineJson = state.optJSONObject("lines") ?: JSONObject()
    val lineKeys = lineJson.keys()
    while (lineKeys.hasNext()) {
        val key = lineKeys.next()
        parseLine(key)?.let { line ->
            lines[line] = if (lineJson.optString(key) == userId) Owner.PLAYER else Owner.BOT
        }
    }
    val boxes = linkedMapOf<Pair<Int, Int>, Owner>()
    val boxJson = state.optJSONObject("boxes") ?: JSONObject()
    val boxKeys = boxJson.keys()
    while (boxKeys.hasNext()) {
        val key = boxKeys.next()
        val parts = key.split(":")
        if (parts.size == 2) boxes[(parts[0].toIntOrNull() ?: 0) to (parts[1].toIntOrNull() ?: 0)] =
            if (boxJson.optString(key) == userId) Owner.PLAYER else Owner.BOT
    }
    val scores = state.optJSONObject("scores") ?: JSONObject()
    return BoardState(
        gridSize = gridSize,
        lines = lines,
        boxes = boxes,
        playerScore = scores.optInt(userId, 0),
        botScore = scores.optInt(opponentId, 0),
        isPlayerTurn = state.optString("turn") == userId,
        isGameOver = state.optString("status") == "FINISHED"
    )
}

private fun opponentDisplayName(json: JSONObject): String {
    val id = json.optString("userId", "")
    val raw = json.optString("displayName", "").trim()
    if (json.optBoolean("isBot", false)) return raw.ifBlank { "Opponent" }
    return raw.takeIf { it.isNotBlank() && it != id && !it.startsWith("guest_") } ?: "Opponent"
}

private fun parseLine(key: String): Line? {
    val endpoints = key.split("|")
    if (endpoints.size != 2) return null
    fun point(value: String): Pair<Int, Int>? {
        val p = value.split(":")
        return if (p.size == 2) (p[0].toIntOrNull() ?: return null) to (p[1].toIntOrNull() ?: return null) else null
    }
    val a = point(endpoints[0]) ?: return null
    val b = point(endpoints[1]) ?: return null
    return if (endpoints[0] <= endpoints[1]) Line(a.first, a.second, b.first, b.second)
    else Line(b.first, b.second, a.first, a.second)
}

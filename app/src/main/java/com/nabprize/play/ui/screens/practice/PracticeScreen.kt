package com.nabprize.play.ui.screens.practice

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import com.nabprize.play.ui.navigation.findActivity
import androidx.compose.ui.viewinterop.AndroidView
import com.nabprize.play.ui.ads.AdMobBanner
import com.nabprize.play.ui.ads.rememberRewardedAdState
import com.nabprize.play.ui.ads.rememberInterstitialAdState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.LocalActivity
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.OndemandVideo
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nabprize.play.ui.components.BoardState
import com.nabprize.play.ui.components.BotColor
import com.nabprize.play.ui.components.DotsAndBoxesBoard
import com.nabprize.play.ui.components.GradientProgressBar
import com.nabprize.play.ui.components.NabPrizeButton
import com.nabprize.play.ui.components.NpCoinImage
import com.nabprize.play.ui.components.Owner
import com.nabprize.play.ui.components.PlayerColor
import com.nabprize.play.ui.components.applyMove
import com.nabprize.play.ui.components.botMove
import com.nabprize.play.ui.theme.AccentGold
import com.nabprize.play.ui.theme.CardWhite
import com.nabprize.play.ui.theme.CreamBackground
import com.nabprize.play.ui.theme.PrimaryOrange
import com.nabprize.play.ui.theme.SecondaryPurple
import com.nabprize.play.ui.theme.StatGreen
import com.nabprize.play.ui.theme.TextPrimary
import com.nabprize.play.ui.theme.TextSecondary
import com.nabprize.play.ui.theme.TextTertiary
import com.nabprize.play.ui.theme.rememberResponsiveMetrics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─── Practice Phases ─────────────────────────────────────────────────────────

private enum class PracticePhase { ENTRY, PLAYING, RESULT }

// Legacy ticket result composables remain below for a future multiplayer release;
// they are not reachable from the MVP flow.
private const val TICKET_CAP = 8
private const val ADS_PER_TICKET = 10

// ─── Practice Screen ─────────────────────────────────────────────────────────

@Composable
fun PracticeScreen(
    modifier: Modifier = Modifier,
    onClaimPracticeReward: (isWin: Boolean, boxesCaptured: Int, onComplete: (Boolean) -> Unit) -> Unit = { _, _, callback -> callback(false) },
    onClaimBonusCoins: (onComplete: (Boolean) -> Unit) -> Unit = { callback -> callback(false) },
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val rewardedAdState = rememberRewardedAdState()
    val interstitialAdState = rememberInterstitialAdState()
    var phase        by remember { mutableStateOf(PracticePhase.ENTRY) }
    var boardState   by remember { mutableStateOf(BoardState()) }
    var playerWon    by remember { mutableStateOf(true) }
    var finalPlayerBoxes by remember { mutableStateOf(0) }
    var finalBotBoxes by remember { mutableStateOf(0) }
    var isWatchingMatchAd by remember { mutableStateOf(false) }
    var hasClaimedMatchReward by remember { mutableStateOf(false) }
    var isWatchingBonusAd by remember { mutableStateOf(false) }
    var hasClaimedBonusAd by remember { mutableStateOf(false) }

    // When it's the bot's turn, schedule a bot move after a short delay
    LaunchedEffect(boardState, phase) {
        if (phase == PracticePhase.PLAYING && !boardState.isPlayerTurn && !boardState.isGameOver) {
            delay(700)
            val move = botMove(boardState)
            boardState = applyMove(boardState, move, Owner.BOT)
        }
        if (boardState.isGameOver && phase == PracticePhase.PLAYING) {
            val won = boardState.playerScore >= boardState.botScore
            playerWon = won
            finalPlayerBoxes = boardState.playerScore
            finalBotBoxes = boardState.botScore
            delay(600)
            phase = PracticePhase.RESULT
        }
    }

    AnimatedContent(
        targetState = phase,
        transitionSpec = {
            (slideInHorizontally { it } + fadeIn()) togetherWith
            (slideOutHorizontally { -it } + fadeOut())
        },
        label = "practice_phase"
    ) { currentPhase ->
        when (currentPhase) {
            PracticePhase.ENTRY -> EntryScreen(
                modifier = modifier,
                onBack = onBack,
                onStart = {
                    boardState = BoardState()
                    finalPlayerBoxes = 0
                    finalBotBoxes = 0
                    hasClaimedMatchReward = false
                    hasClaimedBonusAd = false
                    phase = PracticePhase.PLAYING
                }
            )
            PracticePhase.PLAYING -> PlayingScreen(
                modifier = modifier,
                boardState = boardState,
                onLineClick = { line ->
                    if (boardState.isPlayerTurn && !boardState.isGameOver) {
                        boardState = applyMove(boardState, line, Owner.PLAYER)
                    }
                },
                onBack = { phase = PracticePhase.ENTRY }
            )
            PracticePhase.RESULT -> ResultScreen(
                modifier = modifier,
                playerWon = playerWon,
                boxesCaptured = finalPlayerBoxes,
                botBoxes = finalBotBoxes,
                isWatchingMatchAd = isWatchingMatchAd,
                hasClaimedMatchReward = hasClaimedMatchReward,
                isWatchingBonusAd = isWatchingBonusAd,
                hasClaimedBonusAd = hasClaimedBonusAd,
                onClaimMatchReward = {
                    if (isWatchingMatchAd || hasClaimedMatchReward) return@ResultScreen
                    isWatchingMatchAd = true
                    val activity = context.findActivity()
                    if (activity != null) {
                        rewardedAdState.show(
                            activity = activity,
                            onRewarded = {
                                onClaimPracticeReward(playerWon, finalPlayerBoxes) { success ->
                                    isWatchingMatchAd = false
                                    hasClaimedMatchReward = success
                                }
                            },
                            onUnavailable = {
                                interstitialAdState.show(activity) {
                                    onClaimPracticeReward(playerWon, finalPlayerBoxes) { success ->
                                        isWatchingMatchAd = false
                                        hasClaimedMatchReward = success
                                    }
                                }
                            },
                            onDismissedWithoutReward = {
                                isWatchingMatchAd = false
                            }
                        )
                    } else {
                        onClaimPracticeReward(playerWon, finalPlayerBoxes) { success ->
                            isWatchingMatchAd = false
                            hasClaimedMatchReward = success
                        }
                    }
                },
                onClaimBonusReward = {
                    if (isWatchingBonusAd || hasClaimedBonusAd || !hasClaimedMatchReward) return@ResultScreen
                    isWatchingBonusAd = true
                    val activity = context.findActivity()
                    if (activity != null) {
                        rewardedAdState.show(activity, onRewarded = {
                            onClaimBonusCoins { success ->
                                isWatchingBonusAd = false
                                hasClaimedBonusAd = success
                            }
                        }, onUnavailable = {
                            interstitialAdState.show(activity) {
                                onClaimBonusCoins { success ->
                                    isWatchingBonusAd = false
                                    hasClaimedBonusAd = success
                                }
                            }
                        }, onDismissedWithoutReward = { isWatchingBonusAd = false })
                    } else {
                        onClaimBonusCoins { success ->
                            isWatchingBonusAd = false
                            hasClaimedBonusAd = success
                        }
                    }
                },
                onPlayAgain = {
                    boardState = BoardState()
                    finalPlayerBoxes = 0
                    finalBotBoxes = 0
                    hasClaimedMatchReward = false
                    hasClaimedBonusAd = false
                    phase = PracticePhase.PLAYING
                },
                onHome = onBack
            )
        }
    }
}

// ─── 1. Entry Screen ─────────────────────────────────────────────────────────

private data class TipSlide(
    val emoji: String,
    val title: String,
    val body: String
)

private val tipSlides = listOf(
    TipSlide("🎯", "How to Play",
        "Draw lines between dots to claim boxes. Complete a box and you get another turn!"),
    TipSlide("🤖", "Beat the Bot",
        "NabBot is smart but beatable. Think ahead — avoid giving the bot easy boxes."),
    TipSlide("🏆", "Win Rewards",
        "Beat the bot to earn NP-Coins. The more boxes you claim, the bigger your reward!"),
    TipSlide("💡", "Pro Tip",
        "Watch out for 3-sided boxes — whoever completes the 4th side claims it. Don't set up the bot!"),
    TipSlide("📺", "Claim Your Reward",
        "After each match, watch one short ad to claim the NP-Coins from your boxes."),
    TipSlide("⚡", "Speed Matters",
        "You have 15 seconds per move. Stay sharp — fast thinking beats the bot every time!")
)

@Composable
private fun EntryScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onStart: () -> Unit
) {
    val metrics = rememberResponsiveMetrics()
    val pagerState = rememberPagerState(pageCount = { tipSlides.size })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while (true) {
            delay(3500)
            val nextPage = (pagerState.currentPage + 1) % tipSlides.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CreamBackground)
            .padding(WindowInsets.statusBars.asPaddingValues())
            .padding(WindowInsets.navigationBars.asPaddingValues())
            .verticalScroll(rememberScrollState())
    ) {
        // ── Header ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (metrics.isCompact) 10.dp else 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
            }
            Spacer(Modifier.width(4.dp))
            Column {
                Text("Play & Earn vs AI",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold, color = TextPrimary,
                        fontSize = if (metrics.isCompact) 20.sp else 22.sp))
                Text("Play anytime and earn NP-Coins",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary, fontSize = if (metrics.isCompact) 12.sp else 13.sp))
            }
        }

        // ── Banner Ad ──
        AdMobBanner()

        // ── Main card: You vs NabBot ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (metrics.isCompact) 14.dp else 20.dp, vertical = 10.dp)
                .shadow(4.dp, RoundedCornerShape(20.dp),
                    ambientColor = Color(0x10000000), spotColor = Color(0x10000000))
                .clip(RoundedCornerShape(20.dp))
                .background(CardWhite)
                .padding(if (metrics.isCompact) 18.dp else 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(if (metrics.isCompact) 60.dp else 72.dp)
                        .clip(CircleShape)
                        .background(PrimaryOrange),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.SmartToy, null,
                        modifier = Modifier.size(if (metrics.isCompact) 30.dp else 36.dp), tint = Color.White)
                }

                Spacer(Modifier.height(20.dp))

                Text("You vs NabBot",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = if (metrics.isCompact) 18.sp else 20.sp))

                Spacer(Modifier.height(10.dp))

                Text("5x4 board. Complete a box, take another turn. 15 seconds per move.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary, textAlign = TextAlign.Center, lineHeight = 20.sp),
                    textAlign = TextAlign.Center)

                Spacer(Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(PrimaryOrange)
                        .clickable { onStart() }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚡", fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("Start Playing",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp))
                    }
                }
            }
        }

        // ── Ticket progress card ──
        // ── Tips & Tricks Carousel ──
        Column(modifier = Modifier.padding(top = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (metrics.isCompact) 14.dp else 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("💡 Tips & Tricks",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = if (metrics.isCompact) 15.sp else 16.sp))
                Spacer(Modifier.weight(1f))
                // Dot indicators
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(tipSlides.size) { i ->
                        val isActive = i == pagerState.currentPage
                        Box(
                            modifier = Modifier
                                .size(if (isActive) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (isActive) PrimaryOrange else PrimaryOrange.copy(alpha = 0.25f))
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (metrics.isCompact) 154.dp else 128.dp)
                    .padding(horizontal = if (metrics.isCompact) 12.dp else 20.dp)
            ) { page ->
                val tip = tipSlides[page]
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp)
                        .shadow(4.dp, RoundedCornerShape(20.dp),
                            ambientColor = Color(0x0C000000), spotColor = Color(0x0C000000))
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardWhite)
                        .padding(horizontal = if (metrics.isCompact) 12.dp else 16.dp, vertical = if (metrics.isCompact) 12.dp else 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tip.emoji, fontSize = if (metrics.isCompact) 24.sp else 28.sp)
                        Spacer(Modifier.width(if (metrics.isCompact) 8.dp else 12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tip.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = if (metrics.isCompact) 15.sp else 16.sp))
                            Spacer(Modifier.height(4.dp))
                            Text(tip.body,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextSecondary, lineHeight = if (metrics.isCompact) 17.sp else 19.sp, fontSize = if (metrics.isCompact) 13.sp else 14.sp),
                                maxLines = if (metrics.isCompact) 4 else 3,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))
    }
}

// ─── 2. Playing Screen ───────────────────────────────────────────────────────

@Composable
private fun PlayingScreen(
    modifier: Modifier = Modifier,
    boardState: BoardState,
    onLineClick: (com.nabprize.play.ui.components.Line) -> Unit,
    onBack: () -> Unit
) {
    val isPlayerTurn = boardState.isPlayerTurn

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CreamBackground)
            .padding(WindowInsets.navigationBars.asPaddingValues())
    ) {
        Spacer(Modifier.padding(WindowInsets.statusBars.asPaddingValues()))

        // ── Banner ad (top) ────────────────────────────
        AdMobBanner()

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 18.dp)
        ) {
            Spacer(Modifier.height(10.dp))

            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
                }
                Spacer(Modifier.weight(1f))
                // Animated turn pill
                TurnPill(isPlayerTurn = isPlayerTurn, isGameOver = boardState.isGameOver)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(36.dp))
            }

            Spacer(Modifier.height(12.dp))

            // Score bar
            LiveScoreBar(
                playerScore = boardState.playerScore,
                botScore    = boardState.botScore,
                totalCells  = boardState.totalCells,
                isPlayerTurn = isPlayerTurn
            )

            Spacer(Modifier.height(16.dp))

            // Game board — shadow color follows active player
            val boardShadowColor = if (isPlayerTurn) PlayerColor.copy(0.20f) else BotColor.copy(0.16f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(24.dp),
                        ambientColor = boardShadowColor,
                        spotColor    = boardShadowColor)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                DotsAndBoxesBoard(
                    state = boardState,
                    onLineClick = onLineClick
                )
            }

            Spacer(Modifier.height(16.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendPill(color = PlayerColor, label = "You 🏆")
                Spacer(Modifier.width(16.dp))
                LegendPill(color = BotColor, label = "NabBot 🤖")
            }
        }
    }
}

@Composable
private fun TurnPill(isPlayerTurn: Boolean, isGameOver: Boolean) {
    val infinite = rememberInfiniteTransition(label = "turn_pulse")
    val alpha by infinite.animateFloat(
        0.85f, 1.0f,
        infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pill_alpha"
    )
    val pillColor = when {
        isGameOver   -> AccentGold
        isPlayerTurn -> PlayerColor
        else         -> BotColor
    }
    val pillText = when {
        isGameOver   -> "🏆  Game Over"
        isPlayerTurn -> "●  Your Turn"
        else         -> "•••  Bot Thinking"
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                Brush.horizontalGradient(
                    listOf(pillColor.copy(alpha = alpha * 0.18f), pillColor.copy(alpha = alpha * 0.10f))
                )
            )
            .border(1.5.dp, pillColor.copy(alpha * 0.6f), RoundedCornerShape(50))
            .padding(horizontal = 16.dp, vertical = 7.dp)
    ) {
        Text(
            text = pillText,
            style = MaterialTheme.typography.labelLarge.copy(
                color = pillColor.copy(alpha = alpha),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        )
    }
}

@Composable
private fun LiveScoreBar(playerScore: Int, botScore: Int, totalCells: Int, isPlayerTurn: Boolean) {
    val animatedPlayerScore by animateIntAsState(playerScore, spring(stiffness = Spring.StiffnessMediumLow), label = "p_score")
    val animatedBotScore    by animateIntAsState(botScore,    spring(stiffness = Spring.StiffnessMediumLow), label = "b_score")
    val playerLead = playerScore > botScore

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                5.dp, RoundedCornerShape(20.dp),
                ambientColor = Color(0x14000000), spotColor = Color(0x14000000)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color.White, Color(0xFFFAF6F0))
                )
            )
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Player score
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "$animatedPlayerScore",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Black,
                            color = PlayerColor,
                            fontSize = if (playerLead) 36.sp else 30.sp
                        )
                    )
                    if (playerLead && playerScore > 0) {
                        Text("↑",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = StatGreen, fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                        )
                    }
                }
                Text("You", style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isPlayerTurn) PlayerColor else TextSecondary,
                    fontWeight = if (isPlayerTurn) FontWeight.Bold else FontWeight.Normal
                ))
            }

            // Centre: progress + score divider
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("${playerScore + botScore} / $totalCells boxes",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary))
                Spacer(Modifier.height(6.dp))
                GradientProgressBar(
                    progress = if (totalCells > 0) playerScore.toFloat() / totalCells else 0f,
                    height = 10.dp,
                    modifier = Modifier.fillMaxWidth(),
                    progressColor = Brush.horizontalGradient(listOf(PlayerColor, AccentGold)),
                    trackColor = BotColor.copy(alpha = 0.25f)
                )
            }

            // Bot score
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.Bottom) {
                    if (!playerLead && botScore > 0) {
                        Text("↑",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = BotColor, fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(end = 2.dp, bottom = 4.dp)
                        )
                    }
                    Text(
                        "$animatedBotScore",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Black,
                            color = BotColor,
                            fontSize = if (!playerLead) 36.sp else 30.sp
                        )
                    )
                }
                Text("Bot", style = MaterialTheme.typography.labelSmall.copy(
                    color = if (!isPlayerTurn) BotColor else TextSecondary,
                    fontWeight = if (!isPlayerTurn) FontWeight.Bold else FontWeight.Normal
                ))
            }
        }
    }
}

@Composable
private fun LegendPill(color: Color, label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium.copy(
                color = color, fontWeight = FontWeight.SemiBold))
        }
    }
}

// ─── 3. Result Screen ────────────────────────────────────────────────────────

@Composable
private fun ResultScreen(
    modifier: Modifier = Modifier,
    playerWon: Boolean,
    boxesCaptured: Int = 0,
    botBoxes: Int = 0,
    isWatchingMatchAd: Boolean,
    hasClaimedMatchReward: Boolean,
    isWatchingBonusAd: Boolean,
    hasClaimedBonusAd: Boolean,
    onClaimMatchReward: () -> Unit,
    onClaimBonusReward: () -> Unit,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CreamBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(WindowInsets.navigationBars.asPaddingValues())
    ) {
        Spacer(Modifier.height(52.dp))

        MvpResultContent(
            playerWon = playerWon,
            boxesCaptured = boxesCaptured,
            botBoxes = botBoxes,
            isWatchingMatchAd = isWatchingMatchAd,
            hasClaimedMatchReward = hasClaimedMatchReward,
            isWatchingBonusAd = isWatchingBonusAd,
            hasClaimedBonusAd = hasClaimedBonusAd,
            onClaimMatchReward = onClaimMatchReward,
            onClaimBonusReward = onClaimBonusReward,
            onPlayAgain = onPlayAgain,
            onHome = onHome
        )

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun MvpResultContent(
    playerWon: Boolean,
    boxesCaptured: Int,
    botBoxes: Int,
    isWatchingMatchAd: Boolean,
    hasClaimedMatchReward: Boolean,
    isWatchingBonusAd: Boolean,
    hasClaimedBonusAd: Boolean,
    onClaimMatchReward: () -> Unit,
    onClaimBonusReward: () -> Unit,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit
) {
    ResultHero(playerWon)
    Spacer(Modifier.height(20.dp))
    MatchSummaryCard(playerWon, boxesCaptured, botBoxes, earnedCoins = boxesCaptured)
    Spacer(Modifier.height(20.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(24.dp), ambientColor = PrimaryOrange.copy(0.10f), spotColor = PrimaryOrange.copy(0.10f))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .padding(20.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            NpCoinImage(size = 42.dp)
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (hasClaimedMatchReward) "$boxesCaptured NP-Coins claimed" else "Claim $boxesCaptured NP-Coins",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, color = if (hasClaimedMatchReward) StatGreen else TextPrimary)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (hasClaimedMatchReward) "Your match reward is safely added to your balance."
                else "Watch one short ad to add this match reward to your balance.",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, textAlign = TextAlign.Center),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))
            NabPrizeButton(
                text = when {
                    isWatchingMatchAd -> "Ad playing..."
                    hasClaimedMatchReward -> "✓ Match reward claimed"
                    boxesCaptured == 0 -> "No NP-Coins this match"
                    else -> "Watch Ad & Claim $boxesCaptured NP-Coins"
                },
                onClick = onClaimMatchReward,
                enabled = boxesCaptured > 0 && !isWatchingMatchAd && !hasClaimedMatchReward
            )
        }
    }

    Spacer(Modifier.height(14.dp))
    NabPrizeButton(
        text = when {
            isWatchingBonusAd -> "Ad playing..."
            hasClaimedBonusAd -> "✓ +5 bonus NP-Coins claimed"
            !hasClaimedMatchReward -> "Claim match reward first"
            else -> "Watch Ad for +5 bonus NP-Coins"
        },
        onClick = onClaimBonusReward,
        enabled = hasClaimedMatchReward && !isWatchingBonusAd && !hasClaimedBonusAd,
        backgroundColor = AccentGold,
        contentColor = TextPrimary
    )
    Spacer(Modifier.height(24.dp))
    ActionButtons(onPlayAgain, onHome)
}

// ─── 3a. Normal Result ────────────────────────────────────────────────────────

@Composable
private fun NormalResultContent(
    playerWon: Boolean,
    boxesCaptured: Int = 0,
    botBoxes: Int = 0,
    dailyTicketsEarned: Int = 0,
    adsWatched: Int,
    isWatchingAd: Boolean = false,
    hasClaimedAd: Boolean = false,
    onWatchTicketAd: () -> Unit,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit
) {
    // Win/Loss hero
    ResultHero(playerWon = playerWon)

    Spacer(Modifier.height(20.dp))

    MatchSummaryCard(
        playerWon = playerWon,
        playerBoxes = boxesCaptured,
        botBoxes = botBoxes,
        earnedCoins = boxesCaptured
    )

    Spacer(Modifier.height(20.dp))

    // Direct reward card if won
    if (playerWon) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(20.dp),
                    ambientColor = StatGreen.copy(0.12f), spotColor = StatGreen.copy(0.12f))
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NpCoinImage(size = 36.dp)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("+$boxesCaptured NP-Coins Added!",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold, color = StatGreen, fontSize = 17.sp))
                    Text("Direct reward for beating NabBot 🏆",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    // Ticket progress card
    val adsDone = adsWatched >= ADS_PER_TICKET
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(24.dp),
                ambientColor = PrimaryOrange.copy(0.10f), spotColor = PrimaryOrange.copy(0.10f))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(PrimaryOrange.copy(0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.LocalActivity, null,
                        tint = PrimaryOrange, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Ticket Progress",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold, color = TextPrimary))
                    Text("$dailyTicketsEarned / $TICKET_CAP daily tickets earned",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                }
            }

            Spacer(Modifier.height(18.dp))

            // X/10 pip progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(ADS_PER_TICKET) { i ->
                    val isFilled = i < adsWatched
                    val animatedWeight by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (isFilled) 1.5f else 1.0f,
                        animationSpec = androidx.compose.animation.core.tween(500, delayMillis = i * 50)
                    )
                    Box(
                        modifier = Modifier
                            .weight(animatedWeight)
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isFilled) PrimaryOrange
                                else PrimaryOrange.copy(alpha = 0.15f)
                            )
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = if (adsDone) "🎟  Ticket earned! Tap to collect." else "$adsWatched / $ADS_PER_TICKET ads watched toward next ticket",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (adsDone) StatGreen else TextTertiary,
                    fontWeight = if (adsDone) FontWeight.Bold else FontWeight.Normal)
            )

            Spacer(Modifier.height(16.dp))

            NabPrizeButton(
                text = when {
                    isWatchingAd -> "Ad playing..."
                    hasClaimedAd -> "✓ Reward claimed"
                    adsDone -> "🎟  Collect Ticket"
                    else -> "▶  Watch Ad → Progress Ticket"
                },
                onClick = onWatchTicketAd,
                enabled = !adsDone && !isWatchingAd && !hasClaimedAd,
                backgroundColor = PrimaryOrange
            )
        }
    }

    Spacer(Modifier.height(24.dp))

    ActionButtons(onPlayAgain = onPlayAgain, onHome = onHome)
}

// ─── 3b. Post-Cap Result ──────────────────────────────────────────────────────

@Composable
private fun PostCapResultContent(
    playerWon: Boolean,
    boxesCaptured: Int = 0,
    botBoxes: Int = 0,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit
) {
    // Win/Loss hero
    ResultHero(playerWon = playerWon)

    Spacer(Modifier.height(20.dp))

    MatchSummaryCard(
        playerWon = playerWon,
        playerBoxes = boxesCaptured,
        botBoxes = botBoxes,
        earnedCoins = boxesCaptured
    )

    Spacer(Modifier.height(20.dp))

    // Direct reward card if won
    if (playerWon) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(20.dp),
                    ambientColor = StatGreen.copy(0.12f), spotColor = StatGreen.copy(0.12f))
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NpCoinImage(size = 36.dp)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("+$boxesCaptured NP-Coins Added!",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold, color = StatGreen, fontSize = 17.sp))
                    Text("Direct reward for beating NabBot 🏆",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    // Post-cap notice
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    listOf(AccentGold.copy(0.18f), PrimaryOrange.copy(0.10f))
                )
            )
            .border(1.5.dp, AccentGold.copy(0.4f), RoundedCornerShape(18.dp))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🎟", fontSize = 28.sp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Daily ticket cap reached!",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold, color = TextPrimary))
                Text("You've earned all $TICKET_CAP tickets for today. Come back tomorrow!",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
            }
        }
    }

    Spacer(Modifier.height(24.dp))

    ActionButtons(onPlayAgain = onPlayAgain, onHome = onHome)
}

// ─── Shared result sub-composables ───────────────────────────────────────────

@Composable
private fun ResultHero(playerWon: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                if (playerWon)
                    Brush.linearGradient(listOf(Color(0xFF1B5E20), Color(0xFF2E7D32)))
                else
                    Brush.linearGradient(listOf(Color(0xFF4A148C), Color(0xFF7B1FA2)))
            )
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (playerWon) "🏆" else "😤", fontSize = 56.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                text = if (playerWon) "You Won!" else "You Lost",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (playerWon) "Great game! Here's your reward." else "Keep practising — you'll get there!",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(0.7f),
                    textAlign = TextAlign.Center),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MatchSummaryCard(
    playerWon: Boolean,
    playerBoxes: Int,
    botBoxes: Int,
    earnedCoins: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE8E0D8), RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Text(
            "Match Summary",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ResultPlayerColumn(
                label = "You",
                boxes = playerBoxes,
                isWinner = playerWon,
                icon = Icons.Filled.Person,
                color = PlayerColor
            )
            Text(
                "VS",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = TextTertiary,
                    fontWeight = FontWeight.ExtraBold
                )
            )
            ResultPlayerColumn(
                label = "NabBot",
                boxes = botBoxes,
                isWinner = !playerWon,
                icon = Icons.Filled.SmartToy,
                color = BotColor
            )
        }
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (earnedCoins > 0) StatGreen.copy(alpha = 0.10f) else Color(0xFFF5F5F5))
                .padding(vertical = 11.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (earnedCoins > 0) "You earned $earnedCoins NP-Coins from this match"
                else "You earned 0 NP-Coins from this match",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (earnedCoins > 0) StatGreen else TextSecondary,
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ResultPlayerColumn(
    label: String,
    boxes: Int,
    isWinner: Boolean,
    icon: ImageVector,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
        Text(
            "$boxes boxes",
            style = MaterialTheme.typography.bodySmall.copy(
                color = if (isWinner) color else TextSecondary,
                fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal
            )
        )
    }
}

@Composable
private fun ActionButtons(onPlayAgain: () -> Unit, onHome: () -> Unit) {
    NabPrizeButton(
        text = "▶  Play Again",
        onClick = onPlayAgain,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(12.dp))
    NabPrizeButton(
        text = "← Back to Home",
        onClick = onHome,
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFFF3F4F6),
        contentColor = TextPrimary
    )
}


// ─── Extension to fix border overload ────────────────────────────────────────
private fun Modifier.border(Dp: androidx.compose.ui.unit.Dp, color: Color, shape: androidx.compose.foundation.shape.RoundedCornerShape) = this

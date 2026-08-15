package com.nabprize.play.ui.screens.matchresult

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.outlined.OndemandVideo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nabprize.play.ui.ads.AdMobNativeAd
import com.nabprize.play.ui.components.NabPrizeButton
import com.nabprize.play.ui.components.NpCoinImage
import com.nabprize.play.ui.theme.AccentGold
import com.nabprize.play.ui.theme.CreamBackground
import com.nabprize.play.ui.theme.PrimaryOrange
import com.nabprize.play.ui.theme.SecondaryPurple
import com.nabprize.play.ui.theme.StatGreen
import com.nabprize.play.ui.theme.TextPrimary
import com.nabprize.play.ui.theme.TextSecondary
import com.nabprize.play.ui.theme.TextTertiary

// ─── Screen ──────────────────────────────────────────────────────────────────

@Composable
fun MatchResultScreen(
    modifier: Modifier = Modifier,
    onPlayAgain: () -> Unit = {},
    onPracticeAndEarn: () -> Unit = {},
    onHome: () -> Unit = {},
    onResultRecorded: (isWin: Boolean) -> Unit = {}
) {
    var isWin by remember { mutableStateOf(true) }
    var animateCoins by remember { mutableStateOf(false) }

    // Persist this match exactly once when the result screen is entered.
    LaunchedEffect(Unit) {
        onResultRecorded(isWin)
    }

    val coins = if (isWin) 40 else 2

    // Trigger coin animation on recomposition
    LaunchedEffect(isWin) {
        animateCoins = false
        animateCoins = true
    }

    // Animated coin counter
    val animatedCoinCount by animateFloatAsState(
        targetValue = if (animateCoins) coins.toFloat() else 0f,
        animationSpec = tween(durationMillis = 800, easing = LinearEasing),
        label = "coin_count"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CreamBackground)
            .verticalScroll(rememberScrollState())
            .padding(WindowInsets.navigationBars.asPaddingValues())
    ) {
        Spacer(Modifier.padding(WindowInsets.statusBars.asPaddingValues()))
        Spacer(Modifier.height(20.dp))

        // ── Result hero (with match summary inside) ───────────────
        ResultHero(isWin = isWin)

        Spacer(Modifier.height(20.dp))

        // ── NP-Coins earned card ─────────────────────────────────
        CoinsEarnedCard(coins = coins, animatedCount = animatedCoinCount)

        Spacer(Modifier.height(24.dp))

        // ── Native ad ────────────────────────────────
        AdMobNativeAd(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )

        Spacer(Modifier.height(24.dp))

        // ── Action buttons ────────────────────────────────────────
        NabPrizeButton(
            text = "▶  Play Again",
            onClick = onPlayAgain,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )

        Spacer(Modifier.height(12.dp))

        NabPrizeButton(
            text = "← Back to Home",
            onClick = onHome,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            backgroundColor = Color(0xFFF3F4F6),
            contentColor = TextPrimary
        )

        Spacer(Modifier.height(32.dp))
    }
}

// ─── Result Hero (with match summary inside) ─────────────────────────────────

@Composable
private fun ResultHero(isWin: Boolean) {
    val bgColor1 = if (isWin) Color(0xFF1B5E20) else Color(0xFF4A148C)
    val bgColor2 = if (isWin) Color(0xFF2E7D32) else Color(0xFF7B1FA2)
    val accentColor = if (isWin) StatGreen else SecondaryPurple

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .shadow(10.dp, RoundedCornerShape(28.dp),
                ambientColor = accentColor.copy(0.15f),
                spotColor = accentColor.copy(0.15f))
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(bgColor1, bgColor2)))
            .padding(28.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Trophy / Defeat icon
            Text(if (isWin) "🏆" else "😤", fontSize = 56.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                if (isWin) "Victory!" else "Defeated",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold, color = Color.White
                )
            )
            Text(
                if (isWin) "Outstanding play!" else "Close match!",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(0.65f), textAlign = TextAlign.Center
                ),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            // ── VS matchup inside hero ─────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(0.12f))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // You profile
                    ProfileAvatar(
                        label = "You",
                        score = if (isWin) "5" else "2",
                        avatar = {
                            ProfileIcon(
                                bgGradient = listOf(PrimaryOrange, AccentGold),
                                icon = { Icon(Icons.Default.Person, null, modifier = Modifier.size(22.dp), tint = Color.White) }
                            )
                        },
                        isWinner = isWin,
                        scoreColor = AccentGold
                    )

                    // VS badge
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color.White.copy(0.25f), CircleShape)
                            .background(Color.White.copy(0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("VS", style = MaterialTheme.typography.labelMedium.copy(
                            color = Color.White.copy(0.8f), fontWeight = FontWeight.Black, fontSize = 11.sp))
                    }

                    // Opponent profile
                    ProfileAvatar(
                        label = "Player_X",
                        score = if (isWin) "3" else "4",
                        avatar = {
                            ProfileIcon(
                                bgGradient = listOf(SecondaryPurple, Color(0xFF9C27B0)),
                                icon = { Icon(Icons.Default.SmartToy, null, modifier = Modifier.size(22.dp), tint = Color.White) }
                            )
                        },
                        isWinner = !isWin,
                        scoreColor = SecondaryPurple.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Match stats row inside hero ────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(0.10f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatChip(label = "Boxes", value = if (isWin) "5/9" else "2/9")
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(0.2f)))
                StatChip(label = "Turns", value = if (isWin) "9" else "9")
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(0.2f)))
                StatChip(label = "Result", value = if (isWin) "Won" else "Lost",
                    valueColor = if (isWin) Color(0xFFA5D6A7) else Color(0xFFCE93D8))
            }
        }
    }
}

@Composable
private fun ProfileAvatar(
    label: String,
    score: String,
    avatar: @Composable () -> Unit,
    isWinner: Boolean,
    scoreColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Glow ring for winner
        Box(contentAlignment = Alignment.Center) {
            if (isWinner) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(0.15f))
                )
            }
            avatar()
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium.copy(
            color = Color.White, fontWeight = FontWeight.Bold))
        Text("Score: $score", style = MaterialTheme.typography.labelSmall.copy(
            color = scoreColor, fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun ProfileIcon(
    bgGradient: List<Color>,
    icon: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(bgGradient)),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

@Composable
private fun StatChip(label: String, value: String, valueColor: Color = Color.White) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold, color = valueColor))
        Text(label, style = MaterialTheme.typography.labelSmall.copy(
            color = Color.White.copy(0.55f)))
    }
}

// ─── Coins Earned Card ───────────────────────────────────────────────────────

@Composable
private fun CoinsEarnedCard(coins: Int, animatedCount: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .shadow(6.dp, RoundedCornerShape(24.dp),
                ambientColor = AccentGold.copy(0.10f),
                spotColor = AccentGold.copy(0.10f))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            NpCoinImage(size = 52.dp)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    "You earned",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                )
                Text(
                    "${animatedCount.toInt()} NP-Coins",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = AccentGold
                    )
                )
            }
        }
    }
}




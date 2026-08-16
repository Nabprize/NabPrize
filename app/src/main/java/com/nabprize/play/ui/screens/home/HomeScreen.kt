package com.nabprize.play.ui.screens.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LocalActivity
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nabprize.play.ui.components.DotsAndBoxesPreview
import com.nabprize.play.ui.components.GradientProgressBar
import com.nabprize.play.ui.components.NabPrizeButton
import com.nabprize.play.ui.components.NpCoinImage
import com.nabprize.play.ui.components.OneVsOneIllustration
import com.nabprize.play.R
import com.nabprize.play.ui.theme.AccentGold
import com.nabprize.play.ui.theme.CardWhite
import com.nabprize.play.ui.theme.CreamBackground
import com.nabprize.play.ui.theme.Divider
import com.nabprize.play.ui.theme.PrimaryOrange
import com.nabprize.play.ui.theme.SecondaryPurple
import com.nabprize.play.ui.theme.StatGreen
import com.nabprize.play.ui.theme.TextPrimary
import com.nabprize.play.ui.theme.TextSecondary
import com.nabprize.play.ui.theme.TextTertiary
import com.nabprize.play.ui.theme.rememberResponsiveMetrics

private val REWARD_PER_DAY = listOf(5, 10, 15, 20, 25, 30, 50)

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    username: String = "Player",
    npCoins: Long = 0,
    todayMatchesPlayed: Int = 0,
    todayCoinsEarned: Long = 0,
    checkInDay: Int = 0,
    isCheckedInToday: Boolean = false,
    nextCheckInDay: Int = 1,
    onAvatarClick: () -> Unit = {},
    onNextAchievementClick: () -> Unit = {},
    onPracticeClick: () -> Unit = {},
    onRewardsClick: () -> Unit = {},
    onDailyCheckinClick: () -> Unit = {}
) {
    val metrics = rememberResponsiveMetrics()
    val nextThreshold = 350L
    val achievementProgress = (npCoins.toFloat() / nextThreshold.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CreamBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = metrics.horizontalPadding)
            .padding(WindowInsets.statusBars.asPaddingValues())
            .padding(WindowInsets.navigationBars.asPaddingValues())
    ) {
        Spacer(Modifier.height(if (metrics.isCompact) 12.dp else 16.dp))

        // ── 1. Header ─────────────────────────────────────────────
        HomeHeader(username = username, onAvatarClick = onAvatarClick)

        Spacer(Modifier.height(24.dp))

        // ── 2. Ticket + NP-Coins stat row ─────────────────────────
        NpCoinStatCard(
            modifier = Modifier.fillMaxWidth(),
            value = "$npCoins",
            caption = "earn more by playing"
        )

        Spacer(Modifier.height(if (metrics.isCompact) 14.dp else 18.dp))

        // ── 3. Next Achievement bar ────────────────────────────────
        NextAchievementCard(
            name = "13 FreeFire Diamonds",
            current = npCoins,
            total = nextThreshold,
            progress = achievementProgress,
            onClick = onNextAchievementClick
        )

        Spacer(Modifier.height(18.dp))

        // ── 4. Practice & Earn ────────────────────────────────────
        PracticeCard(onClick = onPracticeClick)

        Spacer(Modifier.height(18.dp))

        // ── 5. Challenge a Player ─────────────────────────────────
        // ── 6. Rewards preview ────────────────────────────────────
        RewardsCard(npCoins = npCoins, onClick = onRewardsClick)

        Spacer(Modifier.height(18.dp))

        // ── 7. Daily Check-in ─────────────────────────────────────
        DailyCheckinCard(
            rewardPerDay  = REWARD_PER_DAY,
            claimedUpTo   = if (isCheckedInToday) checkInDay else nextCheckInDay - 1,
            currentDayIdx = nextCheckInDay,
            todayClaimed  = isCheckedInToday,
            onClick       = onDailyCheckinClick
        )

        Spacer(Modifier.height(18.dp))

        // ── 8. Today's stats ─────────────────────────────────────────
        TodayStatsCard(
            matchesPlayed = todayMatchesPlayed,
            coinsEarned   = todayCoinsEarned.toInt()
        )

        Spacer(Modifier.height(40.dp))
    }
}

// ─── Header ───────────────────────────────────────────────────────────────────

@Composable
private fun HomeHeader(username: String, onAvatarClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Assalam-o-Alaikum",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextSecondary,
                    fontWeight = FontWeight.Normal
                )
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "Hey $username 👋",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(12.dp))
        // Avatar with gradient ring
        Box(
            modifier = Modifier
                .size(50.dp)
                .shadow(elevation = 6.dp, shape = CircleShape, ambientColor = PrimaryOrange.copy(0.18f))
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(PrimaryOrange, AccentGold)
                    )
                )
                .clickable { onAvatarClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile",
                modifier = Modifier.size(28.dp),
                tint = Color.White
            )
        }
    }
}

// ─── Stat Card ────────────────────────────────────────────────────────────────

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    badgeColor: Color,
    label: String,
    value: String,
    valueColor: Color = TextPrimary,
    caption: String
) {
    val metrics = rememberResponsiveMetrics()
    HomeCard(modifier = modifier.heightIn(min = metrics.statCardHeight)) {
        // Badge + label inline
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(color = badgeColor, icon = icon, iconSize = 18.dp, badgeSize = 34.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                color = valueColor,
                fontSize = if (metrics.isCompact) 26.sp else 30.sp
            )
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = caption,
            style = MaterialTheme.typography.bodySmall.copy(color = TextTertiary),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// NP-Coins stat card that uses the real brand logo
@Composable
private fun NpCoinStatCard(
    modifier: Modifier = Modifier,
    value: String,
    caption: String
) {
    val metrics = rememberResponsiveMetrics()
    HomeCard(modifier = modifier.heightIn(min = metrics.statCardHeight)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NpCoinImage(size = 34.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "NP-Coins",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                color = StatGreen,
                fontSize = if (metrics.isCompact) 26.sp else 30.sp
            )
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = caption,
            style = MaterialTheme.typography.bodySmall.copy(color = TextTertiary),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ─── Next Achievement Card ────────────────────────────────────────────────────

@Composable
private fun NextAchievementCard(
    name: String,
    current: Long,
    total: Long,
    progress: Float,
    onClick: () -> Unit
) {
    HomeCard(modifier = Modifier.clickable { onClick() }) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(R.drawable.img_freefire),
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, StatGreen.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Next Reward",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = TextSecondary,
                        letterSpacing = 0.4.sp
                    )
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = name,
style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = StatGreen
            ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            // Percentage pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(StatGreen.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = StatGreen,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        GradientProgressBar(
            progress = progress,
            height = 12.dp,
            modifier = Modifier.fillMaxWidth(),
            progressColor = Brush.horizontalGradient(
                listOf(StatGreen, Color(0xFF388E3C))
            ),
            trackColor = StatGreen.copy(alpha = 0.15f)
        )

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$current / $total NP-Coins",
                style = MaterialTheme.typography.bodySmall.copy(color = TextTertiary)
            )
            Text(
                text = "View Rewards →",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = PrimaryOrange,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

// ─── Practice & Earn Card ─────────────────────────────────────────────────────

@Composable
private fun PracticeCard(onClick: () -> Unit) {
    val metrics = rememberResponsiveMetrics()
    HomeCard(modifier = Modifier.clickable { onClick() }) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Small label pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                listOf(PrimaryOrange.copy(0.18f), SecondaryPurple.copy(0.10f))
                            )
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "🎮  Play & Earn",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = PrimaryOrange,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Practice\n& Earn",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        fontSize = if (metrics.isCompact) 20.sp else 24.sp,
                        lineHeight = if (metrics.isCompact) 24.sp else 28.sp
                    )
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Beat the bot · Watch ads · Earn tickets",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )

                Spacer(Modifier.height(18.dp))

                // CTA
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(listOf(PrimaryOrange, AccentGold))
                        )
                        .padding(horizontal = 18.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = "Start Playing →",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // DotsAndBoxes illustration in a styled container
            Box(
                modifier = Modifier
                    .size(metrics.illustrationSize)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                PrimaryOrange.copy(0.08f),
                                SecondaryPurple.copy(0.10f)
                            )
                        )
                    )
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                DotsAndBoxesPreview(size = if (metrics.isCompact) 76.dp else 96.dp)
            }
        }
    }
}

// ─── Challenge a Player Card ──────────────────────────────────────────────────

@Composable
private fun ChallengeCard(canChallenge: Boolean, onClick: () -> Unit) {
    val metrics = rememberResponsiveMetrics()
    val isComingSoon = !com.nabprize.play.config.FeatureFlags.CHALLENGE_ENABLED
    val isAvailable = canChallenge && !isComingSoon
    HomeCard(
        modifier = Modifier
            .then(if (!isAvailable) Modifier.background(CreamBackground) else Modifier)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Muted overlay when disabled
            if (!isAvailable) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.White.copy(alpha = 0.55f))
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Ticket cost pill top-left
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isAvailable) PrimaryOrange.copy(0.14f)
                                else Color.Gray.copy(0.10f)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.LocalActivity,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = if (isAvailable) PrimaryOrange else TextTertiary
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (isComingSoon) "Coming Soon" else "1 Ticket",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = if (isAvailable) PrimaryOrange else TextTertiary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Challenge\na Player",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isAvailable) TextPrimary else TextTertiary,
                            fontSize = if (metrics.isCompact) 20.sp else 24.sp,
                            lineHeight = if (metrics.isCompact) 24.sp else 28.sp
                        )
                    )

                    Spacer(Modifier.height(6.dp))

                    if (isAvailable) {
                        Text(
                            text = "Challenge 1v1 · Dots & Boxes · Best of 5",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                        Spacer(Modifier.height(18.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(SecondaryPurple, PrimaryOrange)
                                    )
                                )
                                .clickable { onClick() }
                                .padding(horizontal = 18.dp, vertical = 9.dp)
                        ) {
                            Text(
                                text = "Play Now →",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    } else {
                        if (isComingSoon) {
                            Text(
                                text = "Live 1v1 is coming soon",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextTertiary)
                            )
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = "Stay tuned",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = TextTertiary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        } else {
                        Text(
                            text = "You're out of tickets — Play & Earn to get more",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextTertiary)
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "Go Play & Earn →",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = TextTertiary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        }
                    }
                }

                Spacer(Modifier.width(12.dp))

                // 1v1 illustration
                Box(
                    modifier = Modifier
                        .size(metrics.illustrationSize)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                if (isAvailable)
                                    listOf(SecondaryPurple.copy(0.08f), PrimaryOrange.copy(0.08f))
                                else
                                    listOf(Color.Gray.copy(0.06f), Color.Gray.copy(0.06f))
                            )
                        )
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    OneVsOneIllustration(size = if (metrics.isCompact) 78.dp else 100.dp)
                }
            }
        }
    }
}

// ─── Rewards Preview Card ─────────────────────────────────────────────────────

@Composable
private fun RewardsCard(npCoins: Long, onClick: () -> Unit) {
    val rewardItems = listOf(
        Triple(R.drawable.img_freefire, "13 FF Diamonds", 350L),
        Triple(R.drawable.img_mobile_networks_square, "Easyload", 1500L),
        Triple(R.drawable.img_freefire, "FF Diamonds", 3000L),
        Triple(R.drawable.img_earbuds, "Earbuds", 20000L)
    )
    val claimableCount = rewardItems.count { npCoins >= it.third }
    val subtitle = if (claimableCount > 0) {
        "$claimableCount reward${if (claimableCount == 1) "" else "s"} ready to claim"
    } else {
        "${rewardItems.size} rewards to unlock"
    }

    HomeCard(modifier = Modifier.clickable { onClick() }) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                IconBadge(
                    color = AccentGold,
                    icon = Icons.Outlined.EmojiEvents,
                    iconSize = 22.dp,
                    badgeSize = 44.dp
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Rewards",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }
            }
            Text(
                text = "View All →",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = PrimaryOrange,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        Spacer(Modifier.height(16.dp))

        // Mini reward strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            rewardItems.forEach { (imageRes, name, _) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardWhite)
                        .border(1.dp, Divider, RoundedCornerShape(14.dp))
                        .padding(horizontal = 5.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(11.dp))
                            .background(Color(0xFFF7F5F1))
                    ) {
                        Image(
                            painter = painterResource(imageRes),
                            contentDescription = name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(11.dp))
                                .padding(2.dp)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 9.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ─── Daily Check-in Card ──────────────────────────────────────────────────────

@Composable
private fun DailyCheckinCard(
    rewardPerDay: List<Int>,
    claimedUpTo: Int,
    currentDayIdx: Int,
    todayClaimed: Boolean,
    onClick: () -> Unit
) {
    HomeCard {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(
                    color = PrimaryOrange,
                    icon = Icons.Default.CalendarToday,
                    iconSize = 20.dp,
                    badgeSize = 42.dp
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Daily Check-in",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Day $claimedUpTo streak — keep going!",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // 7-day pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            rewardPerDay.forEachIndexed { index, coins ->
                val day = index + 1
                val isClaimed = day <= claimedUpTo
                val isCurrent = day == currentDayIdx && !todayClaimed
                val isFuture  = day > currentDayIdx || (day == currentDayIdx && todayClaimed && false)

                DayPill(
                    day = day,
                    coins = coins,
                    isClaimed = isClaimed,
                    isCurrent = isCurrent,
                    isFuture = isFuture
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        if (todayClaimed) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "✅  Come back tomorrow!",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        } else {
            NabPrizeButton(
                text = "Check In  +${rewardPerDay.getOrElse(currentDayIdx - 1) { 0 }} NP-Coins",
                onClick = onClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DayPill(
    day: Int,
    coins: Int,
    isClaimed: Boolean,
    isCurrent: Boolean,
    isFuture: Boolean
) {
    val metrics = rememberResponsiveMetrics()
    val pillSize = if (metrics.isCompact) 32.dp else 38.dp
    val innerPadding = if (metrics.isCompact) 2.dp else 3.dp
    val dayFontSize = if (metrics.isCompact) 10.sp else 11.sp
    val coinFontSize = if (metrics.isCompact) 8.sp else 9.sp
    val labelFontSize = if (metrics.isCompact) 7.sp else 8.sp
    val circleColor = when {
        isClaimed -> PrimaryOrange
        isCurrent -> AccentGold
        else       -> Color(0xFFEDE6DC)
    }
    val numberColor = when {
        isClaimed -> Color.White
        isCurrent -> TextPrimary
        else       -> TextTertiary
    }
    val coinsColor = when {
        isClaimed -> PrimaryOrange
        isCurrent -> AccentGold
        else       -> TextTertiary
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Glow border for current day
        val outerMod = if (isCurrent)
            Modifier
                .size(pillSize)
                .border(if (metrics.isCompact) 2.dp else 2.5.dp, AccentGold, CircleShape)
                .padding(innerPadding)
        else
            Modifier.size(pillSize).padding(innerPadding)

        Box(
            modifier = outerMod
                .clip(CircleShape)
                .background(circleColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isClaimed) "✓" else "$day",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = numberColor,
                    fontSize = dayFontSize
                )
            )
        }

        Spacer(Modifier.height(5.dp))

        Text(
            text = "+$coins",
            style = MaterialTheme.typography.labelSmall.copy(
                color = coinsColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = coinFontSize
            )
        )
        Text(
            text = "coins",
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextTertiary,
                fontSize = labelFontSize
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ─── Today's Stats Card ───────────────────────────────────────────────────────

@Composable
private fun TodayStatsCard(matchesPlayed: Int, coinsEarned: Int) {
    val metrics = rememberResponsiveMetrics()
    HomeCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Today",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(StatGreen.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Today's activity",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = StatGreen,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TodayStatColumn(
                icon = Icons.Default.SportsEsports,
                tint = PrimaryOrange,
                value = "$matchesPlayed",
                label = "Matches Played"
            )

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(if (metrics.isCompact) 50.dp else 56.dp)
                    .background(Color(0xFFEDE6DC))
            )

            TodayStatColumn(
                icon = Icons.Filled.MonetizationOn,
                tint = AccentGold,
                value = "+$coinsEarned",
                label = "NP-Coins Earned"
            )
        }
    }
}

@Composable
private fun TodayStatColumn(
    icon: ImageVector,
    tint: Color,
    value: String,
    label: String
) {
    val metrics = rememberResponsiveMetrics()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.widthIn(max = if (metrics.isCompact) 124.dp else 150.dp)
    ) {
        Box(
            modifier = Modifier
                .size(if (metrics.isCompact) 38.dp else 44.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(if (metrics.isCompact) 21.dp else 24.dp),
                tint = tint
            )
        }
        Spacer(Modifier.height(if (metrics.isCompact) 6.dp else 8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                fontSize = if (metrics.isCompact) 21.sp else 24.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = TextTertiary,
                fontSize = if (metrics.isCompact) 11.sp else 12.sp
            ),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ─── Shared helpers ───────────────────────────────────────────────────────────

@Composable
private fun HomeCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val metrics = rememberResponsiveMetrics()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .shadow(
                elevation = 5.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color(0x14000000),
                spotColor = Color(0x14000000)
            )
            .clip(RoundedCornerShape(28.dp))
            .background(CardWhite)
            .padding(metrics.cardPadding)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun IconBadge(
    color: Color,
    icon: ImageVector,
    iconSize: Dp = 20.dp,
    badgeSize: Dp = 40.dp
) {
    Box(
        modifier = Modifier
            .size(badgeSize)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(color.copy(alpha = 0.22f), color.copy(alpha = 0.08f))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = color
        )
    }
}

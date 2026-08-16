package com.nabprize.play.ui.screens.rewards

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.nabprize.play.ui.components.FreeFireLogo
import com.nabprize.play.ui.components.GradientProgressBar
import com.nabprize.play.ui.components.JazzLogo
import com.nabprize.play.ui.components.NabPrizeButton
import com.nabprize.play.ui.components.NpCoinImage
import com.nabprize.play.ui.components.PubgLogo
import com.nabprize.play.ui.components.TelenorLogo
import com.nabprize.play.ui.components.UfoneLogo
import com.nabprize.play.ui.theme.AccentGold
import com.nabprize.play.ui.theme.CreamBackground
import com.nabprize.play.ui.theme.PrimaryOrange
import com.nabprize.play.ui.theme.SecondaryPurple
import com.nabprize.play.ui.theme.StatGreen
import com.nabprize.play.ui.theme.TextPrimary
import com.nabprize.play.ui.theme.TextSecondary
import com.nabprize.play.ui.theme.TextTertiary
import com.nabprize.play.ui.theme.CardWhite
import com.nabprize.play.ui.theme.rememberResponsiveMetrics
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import com.nabprize.play.R

// ─── Data Models ─────────────────────────────────────────────────────────────

enum class RewardType { MOBILE_LOAD, GAME_CURRENCY, PHYSICAL_ITEM }

data class RewardTier(
    val id: String,
    val name: String,
    val subtitle: String,
    val cost: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    val mainImageRes: Int? = null,
    val brandImageRes: Int? = null,
    val accentColor: Color,
    val type: RewardType
)

// ─── Mock Data ───────────────────────────────────────────────────────────────

private const val CURRENT_NP_COINS = 640

private val rewardTiers = listOf(
    RewardTier(
        id = "ff_diamonds_13",
        name = "13 FreeFire Diamonds",
        subtitle = "Free Fire · In-game currency",
        cost = 350,
        mainImageRes = R.drawable.img_freefire,
        brandImageRes = null,
        accentColor = Color(0xFFFF3A00),
        type = RewardType.GAME_CURRENCY
    ),
    RewardTier(
        id = "pubg_uc",
        name = "60 PUBG UC",
        subtitle = "PUBG Mobile · In-game currency",
        cost = 3000,
        mainImageRes = R.drawable.img_pubg_mobile,
        accentColor = Color(0xFFFFB800),
        type = RewardType.GAME_CURRENCY
    ),
    RewardTier(
        id = "ff_diamonds",
        name = "100 FreeFire Diamonds",
        subtitle = "Free Fire · In-game currency",
        cost = 3000,
        mainImageRes = R.drawable.img_freefire,
        accentColor = Color(0xFFFF3A00),
        type = RewardType.GAME_CURRENCY
    ),
    RewardTier(
        id = "earbuds",
        name = "Wireless Earbuds",
        subtitle = "Delivered to your doorstep",
        cost = 15000,
        mainImageRes = R.drawable.img_earbuds,
        accentColor = SecondaryPurple,
        type = RewardType.PHYSICAL_ITEM
    )
)

// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun RewardsScreen(
    modifier: Modifier = Modifier,
    npCoins: Long = 0,
    onRedeem: (RewardTier, String, String?, (Boolean, String?) -> Unit) -> Unit = { _, _, _, cb -> cb(true, null) },
    onBack: () -> Unit = {}
) {
    val metrics = rememberResponsiveMetrics()
    var selectedReward by remember { mutableStateOf<RewardTier?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CreamBackground)
            .padding(horizontal = metrics.horizontalPadding)
            .padding(WindowInsets.statusBars.asPaddingValues())
            .padding(WindowInsets.navigationBars.asPaddingValues())
    ) {
        Spacer(Modifier.height(if (metrics.isCompact) 16.dp else 24.dp))

        // ── Top bar ──────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack, "Back",
                    tint = TextPrimary
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "Rewards",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold, color = TextPrimary
                )
            )
        }

        Spacer(Modifier.height(20.dp))

        // ── Balance pill ─────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.horizontalGradient(listOf(PrimaryOrange, AccentGold))
                )
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        "Your Balance",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NpCoinImage(size = 28.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "$npCoins NP-Coins",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                fontSize = if (metrics.isCompact) 22.sp else 26.sp
                            )
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        // ── Rewards list ─────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            rewardTiers.forEach { tier ->
                RewardCard(
                    tier = tier,
                    currentCoins = npCoins.toInt(),
                    onClaim = { selectedReward = tier }
                )
            }
            Spacer(Modifier.height(40.dp))
        }
    }

    // Redemption dialog
    selectedReward?.let { tier ->
        RedemptionDialog(
            reward = tier,
            isSubmitting = isSubmitting,
            onDismiss = { selectedReward = null },
            onSubmit = { primary, secondary ->
                isSubmitting = true
                onRedeem(tier, primary, secondary) { success, err ->
                    isSubmitting = false
                    if (success) {
                        selectedReward = null
                        showSuccessDialog = true
                    } else {
                        errorMessage = err
                    }
                }
            }
        )
    }

    // Success dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            icon = {
                Icon(
                    Icons.Outlined.CheckCircle, null,
                    tint = StatGreen, modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    "Request Submitted!",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    "Your reward claim has been received and is pending admin approval. We will process it within 24 hours.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                NabPrizeButton(
                    text = "Got it!",
                    onClick = { showSuccessDialog = false },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Error dialog
    errorMessage?.let { err ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = {
                Text(
                    "Redemption Failed",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = PrimaryOrange)
                )
            },
            text = {
                Text(
                    err,
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                NabPrizeButton(
                    text = "OK",
                    onClick = { errorMessage = null },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

// ─── Reward Card ──────────────────────────────────────────────────────────────

@Composable
private fun RewardCard(
    tier: RewardTier,
    currentCoins: Int,
    onClaim: () -> Unit
) {
    val metrics = rememberResponsiveMetrics()
    val progress = (currentCoins.toFloat() / tier.cost.toFloat()).coerceIn(0f, 1f)
    val canClaim = currentCoins >= tier.cost

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color(0x18000000),
                spotColor = Color(0x18000000)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .padding(metrics.cardPadding)
    ) {
        Column {
            // ── Header row ───────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                if (tier.mainImageRes != null) {
                    Image(
                        painter = painterResource(id = tier.mainImageRes),
                        contentDescription = null,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF5F5F5)),
                        contentScale = ContentScale.Crop
                    )
                } else if (tier.icon != null) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(tier.accentColor.copy(0.22f), tier.accentColor.copy(0.07f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = tier.icon, contentDescription = null,
                            modifier = Modifier.size(32.dp), tint = tier.accentColor
                        )
                    }
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        tier.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold, color = TextPrimary
                        ),
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        tier.subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary),
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                // Cost pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(tier.accentColor.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        "${tier.cost}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = tier.accentColor, fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Brand logos row ──────────────────────────────────
            if (tier.brandImageRes != null) {
                Spacer(Modifier.height(16.dp))
                Image(
                    painter = painterResource(id = tier.brandImageRes),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
            }

            // ── Progress ─────────────────────────────────────────
            var startAnim by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { startAnim = true }
            val animatedProgress by animateFloatAsState(
                targetValue = if (startAnim) progress else 0f,
                animationSpec = tween(800, delayMillis = 100),
                label = "progress_anim"
            )

            GradientProgressBar(
                progress = animatedProgress,
                height = 14.dp,
                progressColor = Brush.horizontalGradient(
                    listOf(tier.accentColor, tier.accentColor.copy(0.65f))
                ),
                trackColor = tier.accentColor.copy(alpha = 0.10f),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NpCoinImage(size = 14.dp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "$currentCoins / ${tier.cost} NP-Coins",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (canClaim) StatGreen else TextTertiary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
                if (canClaim) {
                    Text(
                        "✓ Ready to claim!",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = StatGreen, fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Claim button ─────────────────────────────────────
            NabPrizeButton(
                text = if (canClaim) "🎁  Claim Reward" else "Keep Earning",
                onClick = onClaim,
                enabled = canClaim,
                backgroundColor = if (canClaim) tier.accentColor else Color(0xFFE0D8D0),
                contentColor = if (canClaim) Color.White else TextTertiary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ─── Redemption Dialog ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RedemptionDialog(
    reward: RewardTier,
    isSubmitting: Boolean = false,
    onDismiss: () -> Unit,
    onSubmit: (primary: String, secondary: String?) -> Unit
) {
    var primaryInput by remember { mutableStateOf("") }
    var secondaryInput by remember { mutableStateOf("") }

    val (primaryLabel, secondaryLabel) = when (reward.type) {
        RewardType.MOBILE_LOAD -> "Mobile Number (e.g. 03XX-XXXXXXX)" to null
        RewardType.GAME_CURRENCY -> "Player ID / UID" to "In-Game Name (IGN)"
        RewardType.PHYSICAL_ITEM -> "Full Name" to "Delivery Address"
    }

    val isValid = primaryInput.isNotBlank() &&
            (secondaryLabel == null || secondaryInput.isNotBlank())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Redeem ${reward.name}",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold, color = TextPrimary
                )
            )
        },
        text = {
            Column {
                Text(
                    "Please provide your details. Our team will process the reward within 24 hours.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = primaryInput,
                    onValueChange = { primaryInput = it },
                    label = { Text(primaryLabel) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = reward.type != RewardType.PHYSICAL_ITEM,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = reward.accentColor,
                        focusedLabelColor = reward.accentColor,
                        cursorColor = reward.accentColor
                    )
                )
                if (secondaryLabel != null) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = secondaryInput,
                        onValueChange = { secondaryInput = it },
                        label = { Text(secondaryLabel) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = reward.type != RewardType.PHYSICAL_ITEM,
                        maxLines = if (reward.type == RewardType.PHYSICAL_ITEM) 3 else 1,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = reward.accentColor,
                            focusedLabelColor = reward.accentColor,
                            cursorColor = reward.accentColor
                        )
                    )
                }
            }
        },
        confirmButton = {
            NabPrizeButton(
                text = if (isSubmitting) "Submitting..." else "Submit Request",
                onClick = {
                    onSubmit(primaryInput.trim(), if (secondaryLabel != null) secondaryInput.trim() else null)
                },
                enabled = isValid && !isSubmitting,
                backgroundColor = reward.accentColor,
                modifier = Modifier.fillMaxWidth()
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge.copy(color = TextSecondary))
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}

package com.nabprize.play.ui.screens.profile

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nabprize.play.ui.components.NpCoinImage
import com.nabprize.play.ui.theme.AccentGold
import com.nabprize.play.ui.theme.CreamBackground
import com.nabprize.play.ui.theme.PrimaryOrange
import com.nabprize.play.ui.theme.SecondaryPurple
import com.nabprize.play.ui.theme.StatGreen
import com.nabprize.play.ui.theme.TextPrimary
import com.nabprize.play.ui.theme.TextSecondary
import com.nabprize.play.ui.theme.rememberResponsiveMetrics

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    displayName: String = "Player",
    username: String = "",
    email: String = "",
    totalWins: Long = 0,
    totalLosses: Long = 0,
    npCoins: Long = 0,
    onBack: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val metrics = rememberResponsiveMetrics()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CreamBackground)
            .padding(horizontal = metrics.horizontalPadding)
            .padding(WindowInsets.statusBars.asPaddingValues())
            .padding(WindowInsets.navigationBars.asPaddingValues())
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(if (metrics.isCompact) 16.dp else 24.dp))

        // ── Header ───────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Profile",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
            )
        }

        Spacer(Modifier.height(32.dp))

        // ── User Info Section ─────────────────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(if (metrics.isCompact) 84.dp else 100.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE5E7EB)),
                contentAlignment = Alignment.Center
            ) {
                // Placeholder avatar image or icon
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    modifier = Modifier.size(if (metrics.isCompact) 42.dp else 50.dp),
                    tint = Color(0xFF9CA3AF)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = displayName.ifBlank { "Player" },
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "@$username",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = PrimaryOrange,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextSecondary
                )
            )
        }

        Spacer(Modifier.height(32.dp))

        // ── Lifetime Stats Summary ────────────────────────────────
        Text(
            text = "Lifetime Stats",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            ),
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProfileStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.SportsEsports,
                iconTint = PrimaryOrange,
                label = "Matches",
                value = "${totalWins + totalLosses}"
            )
            ProfileStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.TrendingUp,
                iconTint = StatGreen,
                label = "Win Record",
                value = "$totalWins W"
            )
            // NP-Coins stat uses the real brand logo
            Box(
                modifier = Modifier
                    .weight(1f)
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = AccentGold.copy(0.3f),
                        spotColor = AccentGold.copy(0.3f)
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color.White, Color(0xFFFFFDF5))
                        )
                    )
                    .border(1.5.dp, AccentGold.copy(0.4f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    NpCoinImage(size = 32.dp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "$npCoins",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            fontSize = 18.sp
                        )
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Coins Earned",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // ── Settings / Account Menu ────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(24.dp),
                    ambientColor = Color(0x14000000),
                    spotColor = Color(0x14000000)
                )
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(vertical = 8.dp)
        ) {
            Column {
                SettingsRow(icon = Icons.Outlined.Person, label = "Account Details")
                HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp, modifier = Modifier.padding(horizontal = 20.dp))
                SettingsRow(icon = Icons.Outlined.Lock, label = "Change Password")
                HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp, modifier = Modifier.padding(horizontal = 20.dp))
                SettingsRow(icon = Icons.Outlined.Notifications, label = "Notifications")
                HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp, modifier = Modifier.padding(horizontal = 20.dp))
                SettingsRow(icon = Icons.AutoMirrored.Outlined.HelpOutline, label = "Help & Support")
                HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp, modifier = Modifier.padding(horizontal = 20.dp))
                SettingsRow(
                    icon = Icons.AutoMirrored.Outlined.Logout,
                    label = "Log Out",
                    iconTint = Color(0xFFEF4444),
                    textColor = Color(0xFFEF4444),
                    onClick = onLogout
                )
            }
        }
        
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun ProfileStatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color(0x14000000),
                spotColor = Color(0x14000000)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(16.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    fontSize = 18.sp
                )
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    iconTint: Color = TextSecondary,
    textColor: Color = TextPrimary,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        )
    }
}

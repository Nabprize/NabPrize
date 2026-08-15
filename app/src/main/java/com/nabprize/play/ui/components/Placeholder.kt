package com.nabprize.play.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nabprize.play.ui.theme.CreamBackground
import com.nabprize.play.ui.theme.PrimaryOrange
import com.nabprize.play.ui.theme.TextSecondary

@Composable
fun PlaceholderScreen(
    title: String,
    emoji: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backIcon: ImageVector = Icons.AutoMirrored.Filled.ArrowBack
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CreamBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (onBack != null) {
            Spacer(Modifier.size(12.dp))
            IconButton(onClick = { onBack() }) {
                Icon(
                    imageVector = backIcon,
                    contentDescription = "Back",
                    tint = PrimaryOrange,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji, style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.size(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = PrimaryOrange,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "Coming Soon",
                style = MaterialTheme.typography.titleMedium.copy(color = androidx.compose.ui.graphics.Color(0xFF9E9E9E))
            )
        }

        Spacer(Modifier.weight(1f))
    }
}

@Composable
fun AccentLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = PrimaryOrange) {
    TextButton(onClick = { onClick() }, modifier = modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = color
            )
        )
    }
}

@Composable
fun CircleAvatar(
    modifier: Modifier = Modifier,
    tint: Color = PrimaryOrange,
    size: Dp = 40.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(tint.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(size * 0.5f),
            tint = tint
        )
    }
}

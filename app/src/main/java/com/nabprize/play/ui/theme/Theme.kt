package com.nabprize.play.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

private val NabPrizeColorScheme = lightColorScheme(
    primary = PrimaryOrange,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryOrangeLight,
    secondary = SecondaryPurple,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryPurpleLight,
    tertiary = AccentGold,
    onTertiary = OnAccent,
    tertiaryContainer = AccentGoldLight,
    background = CreamBackground,
    onBackground = TextPrimary,
    surface = CreamSurface,
    onSurface = TextPrimary,
    surfaceVariant = CardWhite,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    onError = OnPrimary,
    outline = Divider
)

@Composable
fun NabPrizeTheme(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val safeDensity = Density(
        density = density.density,
        fontScale = density.fontScale.coerceIn(0.9f, 1.1f)
    )
    CompositionLocalProvider(LocalDensity provides safeDensity) {
        MaterialTheme(
            colorScheme = NabPrizeColorScheme,
            typography = NabPrizeTypography,
            content = content
        )
    }
}

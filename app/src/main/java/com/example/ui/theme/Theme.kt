package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ObsidianTradingColorScheme = darkColorScheme(
    primary = BuyEmerald,
    onPrimary = OnBuyEmerald,
    primaryContainer = BuyEmeraldContainer,
    onPrimaryContainer = BuyEmeraldBright,
    secondary = AiCyan,
    onSecondary = OnAiCyan,
    secondaryContainer = AiCyanContainer,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = HoldAmber,
    onTertiary = ObsidianBackground,
    tertiaryContainer = HoldAmberContainer,
    onTertiaryContainer = HoldAmber,
    error = SellCoral,
    onError = OnSellCoral,
    errorContainer = SellCoralContainer,
    onErrorContainer = SellCoralBright,
    background = ObsidianBackground,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainer = ObsidianSurface,
    surfaceContainerHigh = ObsidianSurfaceElevated,
    surfaceContainerHighest = ObsidianSurfaceVariant,
    outline = ObsidianBorder,
    outlineVariant = ChartGridLine
)

@Composable
fun SignalTradeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ObsidianTradingColorScheme,
        typography = Typography,
        content = content
    )
}

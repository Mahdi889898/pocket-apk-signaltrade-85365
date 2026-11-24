package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AiTradingSignal
import com.example.model.SignalAction
import com.example.model.formatCryptoPrice
import com.example.ui.theme.AiCyan
import com.example.ui.theme.BuyEmerald
import com.example.ui.theme.BuyEmeraldBright
import com.example.ui.theme.BuyEmeraldContainer
import com.example.ui.theme.HoldAmber
import com.example.ui.theme.HoldAmberContainer
import com.example.ui.theme.MonoTextStyles
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.SellCoral
import com.example.ui.theme.SellCoralBright
import com.example.ui.theme.SellCoralContainer
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@Composable
fun AiSignalResultCard(
    signal: AiTradingSignal,
    onOpenBcGameTerminal: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val accentColor = when (signal.action) {
        SignalAction.BUY -> BuyEmerald
        SignalAction.SELL -> SellCoral
        SignalAction.HOLD -> HoldAmber
    }
    val brightColor = when (signal.action) {
        SignalAction.BUY -> BuyEmeraldBright
        SignalAction.SELL -> SellCoralBright
        SignalAction.HOLD -> HoldAmber
    }
    val containerTint = when (signal.action) {
        SignalAction.BUY -> BuyEmeraldContainer
        SignalAction.SELL -> SellCoralContainer
        SignalAction.HOLD -> HoldAmberContainer
    }
    val actionTitle = when (signal.action) {
        SignalAction.BUY -> stringResource(R.string.signal_buy)
        SignalAction.SELL -> stringResource(R.string.signal_sell)
        SignalAction.HOLD -> stringResource(R.string.signal_hold)
    }
    val actionIcon = when (signal.action) {
        SignalAction.BUY -> Icons.AutoMirrored.Filled.TrendingUp
        SignalAction.SELL -> Icons.AutoMirrored.Filled.TrendingDown
        SignalAction.HOLD -> Icons.Filled.HorizontalRule
    }

    val animatedProgress by animateFloatAsState(
        targetValue = (signal.confidence / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 700),
        label = "confidence_arc"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        containerTint.copy(alpha = 0.42f),
                        ObsidianSurfaceElevated,
                        ObsidianSurface
                    )
                )
            )
            .border(1.2.dp, accentColor.copy(alpha = 0.55f), RoundedCornerShape(22.dp))
            .padding(16.dp)
            .testTag("ai_signal_result_card")
    ) {
        // Header Row: Action Verdict + Confidence Ring
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = AiCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (signal.isFromBcGameVision) {
                            stringResource(R.string.source_bc_vision_ai)
                        } else {
                            "${signal.pairDisplay} • ${signal.timeframeLabel}"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = AiCyan
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = actionIcon,
                            contentDescription = actionTitle,
                            tint = brightColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = actionTitle,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = brightColor
                    )
                }
            }

            // Confidence Circular Gauge
            Box(
                modifier = Modifier.size(66.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(60.dp)) {
                    drawArc(
                        color = ObsidianBorder,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = brightColor,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${signal.confidence}%",
                        style = MonoTextStyles.metricValue,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "اطمینان",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryDark
                    )
                }
            }
        }

        if (signal.trendSummary.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = signal.trendSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimaryDark
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2x2 Price Levels Grid (Entry, Stop Loss, TP1, TP2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PriceLevelCell(
                label = stringResource(R.string.label_entry_price),
                value = formatCryptoPrice(signal.entryPrice),
                accent = AiCyan,
                modifier = Modifier.weight(1f)
            )
            PriceLevelCell(
                label = stringResource(R.string.label_stop_loss),
                value = formatCryptoPrice(signal.stopLoss),
                accent = SellCoralBright,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PriceLevelCell(
                label = stringResource(R.string.label_take_profit_1),
                value = formatCryptoPrice(signal.takeProfit1),
                accent = BuyEmeraldBright,
                modifier = Modifier.weight(1f)
            )
            PriceLevelCell(
                label = stringResource(R.string.label_take_profit_2),
                value = formatCryptoPrice(signal.takeProfit2),
                accent = BuyEmerald,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Risk/Reward & Leverage Badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetaChip(
                label = stringResource(R.string.label_risk_reward),
                value = signal.riskRewardRatio,
                modifier = Modifier.weight(1f)
            )
            MetaChip(
                label = stringResource(R.string.label_leverage),
                value = signal.suggestedLeverage,
                modifier = Modifier.weight(1f)
            )
        }

        // Technical Reasoning Bullets
        if (signal.reasoningBullets.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = ObsidianBorder.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.label_ai_reasoning),
                style = MaterialTheme.typography.titleSmall,
                color = AiCyan
            )
            Spacer(modifier = Modifier.height(8.dp))
            signal.reasoningBullets.forEach { bullet ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 7.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(brightColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = bullet,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimaryDark.copy(alpha = 0.92f)
                    )
                }
            }
        }

        // Jump to BC.Game Contract Terminal button
        if (onOpenBcGameTerminal != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onOpenBcGameTerminal,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("open_bc_game_terminal_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = if (signal.action == SignalAction.SELL) Color.White else Color(0xFF022C22)
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.OpenInBrowser,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.btn_open_in_bcgame),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun PriceLevelCell(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(ObsidianSurface.copy(alpha = 0.85f))
            .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondaryDark,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(4.dp))
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Text(
                text = value,
                style = MonoTextStyles.metricValue,
                color = accent,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MetaChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianSurface.copy(alpha = 0.7f))
            .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondaryDark
        )
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Text(
                text = value,
                style = MonoTextStyles.compactTicker.copy(fontWeight = FontWeight.Bold),
                color = TextPrimaryDark
            )
        }
    }
}

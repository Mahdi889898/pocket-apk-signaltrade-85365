package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AiTradingSignal
import com.example.model.CryptoPair
import com.example.model.ScannerSignalItem
import com.example.model.ScannerUiState
import com.example.model.SignalAction
import com.example.model.formatCryptoPrice
import com.example.model.formatPercent
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
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ScannerHistoryScreen(
    scannerState: ScannerUiState,
    signalHistory: List<AiTradingSignal>,
    timeframeLabel: String,
    onRunScanner: () -> Unit,
    onInspectPair: (CryptoPair) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSection by remember { mutableIntStateOf(0) } // 0 = Scanner, 1 = History
    var actionFilter by remember { mutableStateOf<SignalAction?>(null) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Segmented Switch: AI Scanner vs Signal History
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ObsidianSurface)
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SectionTabButton(
                        title = stringResource(R.string.tab_ai_scanner),
                        icon = Icons.Filled.Radar,
                        selected = selectedSection == 0,
                        onClick = { selectedSection = 0 },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("section_tab_scanner")
                    )
                    SectionTabButton(
                        title = "${stringResource(R.string.tab_signal_history)} (${signalHistory.size})",
                        icon = Icons.Filled.History,
                        selected = selectedSection == 1,
                        onClick = { selectedSection = 1 },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("section_tab_history")
                    )
                }
            }

            if (selectedSection == 0) {
                // Scanner Header + Run CTA
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(ObsidianSurfaceElevated)
                            .border(1.dp, ObsidianBorder, RoundedCornerShape(20.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "${stringResource(R.string.scanner_title)} ($timeframeLabel)",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = stringResource(R.string.scanner_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryDark
                        )

                        val isScanning = scannerState is ScannerUiState.Loading
                        Button(
                            onClick = onRunScanner,
                            enabled = !isScanning,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("run_ai_scanner_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AiCyan,
                                contentColor = Color(0xFF061524)
                            )
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF061524)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.btn_scanning_market),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.btn_run_scanner),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }

                // Filter Pills
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterPill(
                            label = stringResource(R.string.filter_all),
                            selected = actionFilter == null,
                            accent = AiCyan,
                            onClick = { actionFilter = null }
                        )
                        FilterPill(
                            label = stringResource(R.string.filter_buy),
                            selected = actionFilter == SignalAction.BUY,
                            accent = BuyEmeraldBright,
                            onClick = { actionFilter = SignalAction.BUY }
                        )
                        FilterPill(
                            label = stringResource(R.string.filter_sell),
                            selected = actionFilter == SignalAction.SELL,
                            accent = SellCoralBright,
                            onClick = { actionFilter = SignalAction.SELL }
                        )
                    }
                }

                when (scannerState) {
                    is ScannerUiState.Idle -> {
                        item {
                            EmptyPanel(message = stringResource(R.string.empty_scanner_hint))
                        }
                    }

                    is ScannerUiState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(color = AiCyan)
                                    Text(
                                        text = "در حال دریافت کندل‌های ۸ ارز و تحلیل با هوش مصنوعی…",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondaryDark
                                    )
                                }
                            }
                        }
                    }

                    is ScannerUiState.Error -> {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SellCoralContainer.copy(alpha = 0.45f))
                                    .border(1.dp, SellCoral, RoundedCornerShape(16.dp))
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.WarningAmber,
                                    contentDescription = null,
                                    tint = SellCoralBright
                                )
                                Text(
                                    text = scannerState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimaryDark
                                )
                            }
                        }
                    }

                    is ScannerUiState.Success -> {
                        val filtered = if (actionFilter == null) {
                            scannerState.items
                        } else {
                            scannerState.items.filter { it.action == actionFilter }
                        }
                        if (filtered.isEmpty()) {
                            item {
                                EmptyPanel(message = "سیگنالی با این فیلتر یافت نشد.")
                            }
                        } else {
                            items(filtered, key = { it.pair.symbol }) { item ->
                                ScannerItemCard(
                                    item = item,
                                    onClick = { onInspectPair(item.pair) }
                                )
                            }
                        }
                    }
                }
            } else {
                // Signal History Tab
                if (signalHistory.isEmpty()) {
                    item {
                        EmptyPanel(message = stringResource(R.string.empty_history_hint))
                    }
                } else {
                    items(signalHistory, key = { it.id }) { historyItem ->
                        HistorySignalCard(signal = historyItem)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTabButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) ObsidianSurfaceElevated else Color.Transparent)
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) AiCyan.copy(alpha = 0.6f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) AiCyan else TextSecondaryDark,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (selected) TextPrimaryDark else TextSecondaryDark,
            maxLines = 1
        )
    }
}

@Composable
private fun FilterPill(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) accent.copy(alpha = 0.2f) else ObsidianSurface)
            .border(
                width = 1.dp,
                color = if (selected) accent else ObsidianBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) accent else TextSecondaryDark
        )
    }
}

@Composable
private fun ScannerItemCard(
    item: ScannerSignalItem,
    onClick: () -> Unit
) {
    val accent = when (item.action) {
        SignalAction.BUY -> BuyEmeraldBright
        SignalAction.SELL -> SellCoralBright
        SignalAction.HOLD -> HoldAmber
    }
    val badgeBg = when (item.action) {
        SignalAction.BUY -> BuyEmeraldContainer
        SignalAction.SELL -> SellCoralContainer
        SignalAction.HOLD -> HoldAmberContainer
    }
    val actionLabel = when (item.action) {
        SignalAction.BUY -> "BUY"
        SignalAction.SELL -> "SELL"
        SignalAction.HOLD -> "HOLD"
    }
    val icon = when (item.action) {
        SignalAction.BUY -> Icons.AutoMirrored.Filled.TrendingUp
        SignalAction.SELL -> Icons.AutoMirrored.Filled.TrendingDown
        SignalAction.HOLD -> Icons.Filled.HorizontalRule
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(ObsidianSurface)
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
            .testTag("scanner_item_${item.pair.baseAsset.lowercase(Locale.ROOT)}"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(
                        text = item.pair.displayPair,
                        style = MonoTextStyles.metricValue.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimaryDark
                    )
                }
                Text(
                    text = item.pair.displayNameFa,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondaryDark
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(badgeBg)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(15.dp)
                )
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(
                        text = "$actionLabel ${item.confidence}%",
                        style = MonoTextStyles.compactTicker.copy(fontWeight = FontWeight.Bold),
                        color = accent
                    )
                }
            }
        }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Price: ${formatCryptoPrice(item.currentPrice)} (${formatPercent(item.changePercent24h)})",
                    style = MonoTextStyles.compactTicker,
                    color = TextSecondaryDark
                )
                Text(
                    text = "RSI: ${String.format(Locale.US, "%.1f", item.rsi14)}",
                    style = MonoTextStyles.compactTicker,
                    color = AiCyan
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ObsidianSurfaceElevated)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Entry ${formatCryptoPrice(item.entryPrice)}", style = MonoTextStyles.microAxis, color = AiCyan)
                Text("TP ${formatCryptoPrice(item.takeProfit)}", style = MonoTextStyles.microAxis, color = BuyEmeraldBright)
                Text("SL ${formatCryptoPrice(item.stopLoss)}", style = MonoTextStyles.microAxis, color = SellCoralBright)
            }
        }

        Text(
            text = item.shortReason,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimaryDark.copy(alpha = 0.9f)
        )
    }
}

@Composable
private fun HistorySignalCard(signal: AiTradingSignal) {
    val accent = when (signal.action) {
        SignalAction.BUY -> BuyEmeraldBright
        SignalAction.SELL -> SellCoralBright
        SignalAction.HOLD -> HoldAmber
    }
    val timeStr = remember(signal.analyzedAtMs) {
        SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(signal.analyzedAtMs))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ObsidianSurface)
            .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = signal.pairDisplay,
                    style = MonoTextStyles.metricValue,
                    color = TextPrimaryDark
                )
                Text(
                    text = signal.timeframeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = AiCyan
                )
            }
            Text(
                text = "${signal.action.name} • ${signal.confidence}% • $timeStr",
                style = MonoTextStyles.compactTicker.copy(fontWeight = FontWeight.Bold),
                color = accent
            )
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Entry: ${formatCryptoPrice(signal.entryPrice)}", style = MonoTextStyles.microAxis, color = AiCyan)
                Text("TP1: ${formatCryptoPrice(signal.takeProfit1)}", style = MonoTextStyles.microAxis, color = BuyEmerald)
                Text("SL: ${formatCryptoPrice(signal.stopLoss)}", style = MonoTextStyles.microAxis, color = SellCoral)
            }
        }
        if (signal.trendSummary.isNotBlank()) {
            Text(
                text = signal.trendSummary,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryDark
            )
        }
    }
}

@Composable
private fun EmptyPanel(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(ObsidianSurface)
            .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = TextMutedDark
        )
    }
}

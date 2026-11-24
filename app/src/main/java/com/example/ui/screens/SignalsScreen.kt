package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CryptoPair
import com.example.model.MarketDataState
import com.example.model.SUPPORTED_CONTRACT_PAIRS
import com.example.model.SignalUiState
import com.example.model.TechnicalIndicators
import com.example.model.Timeframe
import com.example.model.formatCryptoPrice
import com.example.model.formatPercent
import com.example.ui.components.AiSignalResultCard
import com.example.ui.components.InteractiveCandlestickChart
import com.example.ui.theme.AiCyan
import com.example.ui.theme.BuyEmerald
import com.example.ui.theme.BuyEmeraldBright
import com.example.ui.theme.BuyEmeraldContainer
import com.example.ui.theme.HoldAmber
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
import com.example.viewmodel.TradingUiState
import java.util.Locale

@Composable
fun SignalsScreen(
    state: TradingUiState,
    onSelectPair: (CryptoPair) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSubmitCustomSymbol: (String) -> Unit,
    onSelectTimeframe: (Timeframe) -> Unit,
    onRefreshMarket: () -> Unit,
    onAnalyzeWithAi: () -> Unit,
    onOpenBcGameTerminal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredPairs = if (state.searchQuery.isBlank()) {
        SUPPORTED_CONTRACT_PAIRS
    } else {
        val q = state.searchQuery.trim().lowercase(Locale.ROOT)
        SUPPORTED_CONTRACT_PAIRS.filter {
            it.baseAsset.lowercase(Locale.ROOT).contains(q) ||
                it.displayNameFa.contains(q)
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Connection Status Banner (BC.Game Contract Link)
            item {
                BcGameStatusHeader(
                    bcGameReachable = state.bcGameReachable,
                    onRefresh = onRefreshMarket,
                    onOpenTerminal = onOpenBcGameTerminal
                )
            }

            // Pair Search & Custom Contract Ticker Input
            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pair_search_input"),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search_pair_placeholder),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMutedDark
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = AiCyan
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ObsidianSurfaceElevated,
                        unfocusedContainerColor = ObsidianSurface,
                        focusedBorderColor = AiCyan,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (state.searchQuery.isNotBlank()) {
                                onSubmitCustomSymbol(state.searchQuery)
                            }
                        }
                    )
                )
            }

            // Horizontal Crypto Pair Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filteredPairs, key = { it.symbol }) { pair ->
                        val isSelected = pair.symbol == state.selectedPair.symbol
                        PairChip(
                            pair = pair,
                            selected = isSelected,
                            onClick = { onSelectPair(pair) }
                        )
                    }
                }
            }

            // Live Price Hero + Timeframe Switcher + Candlestick Chart
            item {
                when (val market = state.marketState) {
                    is MarketDataState.Loading -> {
                        MarketLoadingCard(pair = state.selectedPair)
                    }

                    is MarketDataState.Error -> {
                        MarketErrorCard(
                            message = market.message,
                            onRetry = onRefreshMarket
                        )
                    }

                    is MarketDataState.Ready -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            LivePriceHeroRow(
                                pair = state.selectedPair,
                                lastPrice = market.ticker.lastPrice,
                                changePct = market.ticker.changePercent24h,
                                high24h = market.ticker.high24h,
                                low24h = market.ticker.low24h
                            )

                            TimeframeSelectorRow(
                                selected = state.selectedTimeframe,
                                onSelect = onSelectTimeframe
                            )

                            val activeSignal = (state.signalState as? SignalUiState.Success)?.signal
                            InteractiveCandlestickChart(
                                candles = market.candles,
                                indicators = market.indicators,
                                activeSignal = activeSignal
                            )

                            IndicatorsStrip(indicators = market.indicators)
                        }
                    }
                }
            }

            // Primary AI Buy/Sell Analyze CTA
            item {
                val isAnalyzing = state.signalState is SignalUiState.Loading
                val isMarketReady = state.marketState is MarketDataState.Ready
                Button(
                    onClick = onAnalyzeWithAi,
                    enabled = isMarketReady && !isAnalyzing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("analyze_ai_signal_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AiCyan,
                        contentColor = Color(0xFF061524),
                        disabledContainerColor = ObsidianSurfaceElevated,
                        disabledContentColor = TextMutedDark
                    )
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.5.dp,
                            color = AiCyan
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.btn_analyzing_ai),
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
                            text = stringResource(R.string.btn_analyze_ai),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // AI Signal Result / Error / Empty Hint
            item {
                when (val sigState = state.signalState) {
                    is SignalUiState.Idle -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(ObsidianSurface)
                                .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.empty_signal_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondaryDark
                            )
                        }
                    }

                    is SignalUiState.Loading -> {
                        // Progress is already shown inside CTA; show subtle analysis status card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(ObsidianSurfaceElevated)
                                .border(1.dp, AiCyan.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp,
                                color = AiCyan
                            )
                            Text(
                                text = "در حال بررسی الگوی کندل‌ها، RSI، EMA و نقاط حمایت/مقاومت برای ${state.selectedPair.displayPair}…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimaryDark
                            )
                        }
                    }

                    is SignalUiState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(SellCoralContainer.copy(alpha = 0.45f))
                                .border(1.dp, SellCoral.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.WarningAmber,
                                    contentDescription = null,
                                    tint = SellCoralBright
                                )
                                Text(
                                    text = sigState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimaryDark
                                )
                            }
                            OutlinedButton(
                                onClick = onAnalyzeWithAi,
                                modifier = Modifier.testTag("retry_ai_signal_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(text = stringResource(R.string.action_retry))
                            }
                        }
                    }

                    is SignalUiState.Success -> {
                        AnimatedVisibility(visible = true) {
                            AiSignalResultCard(
                                signal = sigState.signal,
                                onOpenBcGameTerminal = onOpenBcGameTerminal
                            )
                        }
                    }
                }
            }

            // Educational Disclaimer
            item {
                Text(
                    text = stringResource(R.string.disclaimer_text),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMutedDark,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun BcGameStatusHeader(
    bcGameReachable: Boolean?,
    onRefresh: () -> Unit,
    onOpenTerminal: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(ObsidianSurfaceElevated, ObsidianSurface)
                )
            )
            .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onOpenTerminal)
                .padding(vertical = 4.dp)
                .testTag("header_bc_game_link"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        when (bcGameReachable) {
                            true -> BuyEmeraldBright
                            false -> HoldAmber
                            null -> AiCyan
                        }
                    )
            )
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimaryDark
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Link,
                        contentDescription = null,
                        tint = BuyEmeraldBright,
                        modifier = Modifier.size(13.dp)
                    )
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            text = stringResource(R.string.bc_contract_url_label),
                            style = MonoTextStyles.microAxis,
                            color = BuyEmeraldBright
                        )
                    }
                }
            }
        }

        IconButton(
            onClick = onRefresh,
            modifier = Modifier
                .size(48.dp)
                .testTag("refresh_market_button")
        ) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = stringResource(R.string.action_refresh),
                tint = AiCyan
            )
        }
    }
}

@Composable
private fun PairChip(
    pair: CryptoPair,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) BuyEmeraldContainer else ObsidianSurface)
            .border(
                width = 1.dp,
                color = if (selected) BuyEmerald else ObsidianBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp)
            .testTag("pair_chip_${pair.baseAsset.lowercase(Locale.ROOT)}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Text(
                text = pair.baseAsset,
                style = MonoTextStyles.metricValue,
                color = if (selected) BuyEmeraldBright else TextPrimaryDark
            )
        }
        Text(
            text = pair.displayNameFa,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) TextPrimaryDark else TextSecondaryDark
        )
    }
}

@Composable
private fun LivePriceHeroRow(
    pair: CryptoPair,
    lastPrice: Double,
    changePct: Double,
    high24h: Double,
    low24h: Double
) {
    val isPositive = changePct >= 0.0
    val changeColor = if (isPositive) BuyEmeraldBright else SellCoralBright
    val changeBg = if (isPositive) BuyEmeraldContainer else SellCoralContainer

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(
                        text = pair.displayPair,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimaryDark
                    )
                }
                Text(
                    text = "قرارداد دائمی",
                    style = MaterialTheme.typography.labelSmall,
                    color = AiCyan,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AiCyan.copy(alpha = 0.14f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatCryptoPrice(lastPrice),
                        style = MonoTextStyles.priceHero,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = formatPercent(changePct),
                        style = MonoTextStyles.compactTicker.copy(fontWeight = FontWeight.Bold),
                        color = changeColor,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(changeBg)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${stringResource(R.string.label_24h_high)}: ${formatCryptoPrice(high24h)}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondaryDark
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${stringResource(R.string.label_24h_low)}: ${formatCryptoPrice(low24h)}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondaryDark
            )
        }
    }
}

@Composable
private fun TimeframeSelectorRow(
    selected: Timeframe,
    onSelect: (Timeframe) -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(ObsidianSurface)
                .border(1.dp, ObsidianBorder, RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Timeframe.entries.forEach { tf ->
                val active = tf == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (active) ObsidianSurfaceElevated else Color.Transparent)
                        .border(
                            width = if (active) 1.dp else 0.dp,
                            color = if (active) AiCyan.copy(alpha = 0.6f) else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelect(tf) }
                        .testTag("timeframe_${tf.label}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tf.label,
                        style = MonoTextStyles.compactTicker.copy(
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (active) AiCyan else TextSecondaryDark
                    )
                }
            }
        }
    }
}

@Composable
private fun IndicatorsStrip(indicators: TechnicalIndicators) {
    val rsiColor = when {
        indicators.rsi14 <= 32.0 -> BuyEmeraldBright
        indicators.rsi14 >= 68.0 -> SellCoralBright
        else -> TextPrimaryDark
    }
    val emaBullish = indicators.ema9 >= indicators.ema21

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IndicatorMiniCard(
            title = "RSI (14)",
            value = String.format(Locale.US, "%.1f", indicators.rsi14),
            accent = rsiColor,
            modifier = Modifier.weight(1f)
        )
        IndicatorMiniCard(
            title = "روند EMA",
            value = if (emaBullish) "صعودی" else "نزولی",
            accent = if (emaBullish) BuyEmeraldBright else SellCoralBright,
            modifier = Modifier.weight(1f)
        )
        IndicatorMiniCard(
            title = stringResource(R.string.label_support),
            value = formatCryptoPrice(indicators.supportLevel),
            accent = BuyEmerald,
            modifier = Modifier.weight(1f)
        )
        IndicatorMiniCard(
            title = stringResource(R.string.label_resistance),
            value = formatCryptoPrice(indicators.resistanceLevel),
            accent = SellCoral,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun IndicatorMiniCard(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(ObsidianSurface)
            .border(1.dp, ObsidianBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondaryDark,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MonoTextStyles.compactTicker.copy(fontWeight = FontWeight.Bold),
            color = accent,
            maxLines = 1
        )
    }
}

@Composable
private fun MarketLoadingCard(pair: CryptoPair) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(ObsidianSurface)
            .border(1.dp, ObsidianBorder, RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(color = AiCyan)
            Text(
                text = "در حال دریافت کندل‌های زنده ${pair.displayPair}…",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondaryDark
            )
        }
    }
}

@Composable
private fun MarketErrorCard(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SellCoralContainer.copy(alpha = 0.35f))
            .border(1.dp, SellCoral.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.WarningAmber,
            contentDescription = null,
            tint = SellCoralBright,
            modifier = Modifier.size(32.dp)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimaryDark
        )
        Button(
            onClick = onRetry,
            modifier = Modifier.testTag("retry_market_fetch_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = stringResource(R.string.action_retry))
        }
    }
}

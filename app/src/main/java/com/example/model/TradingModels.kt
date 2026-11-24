package com.example.model

import java.util.Locale

data class CryptoPair(
    val baseAsset: String,
    val quoteAsset: String = "USDT",
    val displayNameFa: String
) {
    val symbol: String get() = "$baseAsset$quoteAsset"
    val displayPair: String get() = "$baseAsset/$quoteAsset"
    val okxSwapInstId: String get() = "$baseAsset-$quoteAsset-SWAP"
}

val SUPPORTED_CONTRACT_PAIRS = listOf(
    CryptoPair("BTC", "USDT", "بیت‌کوین"),
    CryptoPair("ETH", "USDT", "اتریوم"),
    CryptoPair("SOL", "USDT", "سولانا"),
    CryptoPair("BNB", "USDT", "بایننس کوین"),
    CryptoPair("XRP", "USDT", "ریپل"),
    CryptoPair("DOGE", "USDT", "دوج‌کوین"),
    CryptoPair("TON", "USDT", "تون‌کوین"),
    CryptoPair("ADA", "USDT", "کاردانو"),
    CryptoPair("AVAX", "USDT", "آوالانچ"),
    CryptoPair("LINK", "USDT", "چین‌لینک"),
    CryptoPair("SUI", "USDT", "سویی"),
    CryptoPair("PEPE", "USDT", "پپه")
)

enum class Timeframe(
    val label: String,
    val okxBar: String,
    val binanceInterval: String
) {
    M1("1m", "1m", "1m"),
    M5("5m", "5m", "5m"),
    M15("15m", "15m", "15m"),
    H1("1H", "1H", "1h"),
    H4("4H", "4H", "4h"),
    D1("1D", "1D", "1d")
}

data class Candle(
    val openTimeMs: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
) {
    val isBullish: Boolean get() = close >= open
}

data class TickerSnapshot(
    val pair: CryptoPair,
    val lastPrice: Double,
    val changePercent24h: Double,
    val high24h: Double,
    val low24h: Double,
    val volume24h: Double,
    val feedName: String
)

data class TechnicalIndicators(
    val rsi14: Double,
    val ema9: Double,
    val ema21: Double,
    val ema50: Double,
    val macdLine: Double,
    val macdSignal: Double,
    val macdHistogram: Double,
    val bollingerUpper: Double,
    val bollingerMiddle: Double,
    val bollingerLower: Double,
    val atr14: Double,
    val supportLevel: Double,
    val resistanceLevel: Double,
    val ema9Series: List<Double>,
    val ema21Series: List<Double>
)

enum class SignalAction {
    BUY,
    SELL,
    HOLD
}

data class ChartMarker(
    val candleIndex: Int,
    val action: SignalAction,
    val price: Double,
    val label: String
)

data class AiTradingSignal(
    val id: String,
    val pairDisplay: String,
    val timeframeLabel: String,
    val action: SignalAction,
    val confidence: Int,
    val entryPrice: Double,
    val stopLoss: Double,
    val takeProfit1: Double,
    val takeProfit2: Double,
    val riskRewardRatio: String,
    val suggestedLeverage: String,
    val trendSummary: String,
    val reasoningBullets: List<String>,
    val chartMarkers: List<ChartMarker>,
    val isFromBcGameVision: Boolean,
    val analyzedAtMs: Long
)

data class ScannerSignalItem(
    val pair: CryptoPair,
    val currentPrice: Double,
    val changePercent24h: Double,
    val rsi14: Double,
    val action: SignalAction,
    val confidence: Int,
    val entryPrice: Double,
    val stopLoss: Double,
    val takeProfit: Double,
    val shortReason: String
)

sealed interface MarketDataState {
    data object Loading : MarketDataState
    data class Ready(
        val ticker: TickerSnapshot,
        val candles: List<Candle>,
        val indicators: TechnicalIndicators
    ) : MarketDataState
    data class Error(val message: String) : MarketDataState
}

sealed interface SignalUiState {
    data object Idle : SignalUiState
    data object Loading : SignalUiState
    data class Success(val signal: AiTradingSignal) : SignalUiState
    data class Error(val message: String) : SignalUiState
}

sealed interface ScannerUiState {
    data object Idle : ScannerUiState
    data object Loading : ScannerUiState
    data class Success(val items: List<ScannerSignalItem>, val scannedAtMs: Long) : ScannerUiState
    data class Error(val message: String) : ScannerUiState
}

enum class AppTab {
    SIGNALS,
    BC_TERMINAL,
    SCANNER_HISTORY
}

fun formatCryptoPrice(price: Double): String {
    if (!price.isFinite() || price <= 0.0) return "—"
    return when {
        price >= 1000.0 -> String.format(Locale.US, "$%,.2f", price)
        price >= 1.0 -> String.format(Locale.US, "$%,.4f", price)
        price >= 0.01 -> String.format(Locale.US, "$%.5f", price)
        else -> String.format(Locale.US, "$%.8f", price)
    }
}

fun formatPercent(value: Double): String {
    if (!value.isFinite()) return "0.00%"
    val sign = if (value >= 0) "+" else ""
    return String.format(Locale.US, "%s%.2f%%", sign, value)
}

package com.example.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.data.MarketBundle
import com.example.model.AiTradingSignal
import com.example.model.ChartMarker
import com.example.model.ScannerSignalItem
import com.example.model.SignalAction
import com.example.model.Timeframe
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.Locale
import java.util.UUID

private const val TAG = "TradingAiService"
private const val GEMINI_MODEL = "gemini-3.8-flash"

class TradingAiService {

    private val singleSignalSchema = Schema.obj(
        properties = mapOf(
            "pairDisplay" to Schema.string(description = "Detected or analyzed trading pair such as BTC/USDT"),
            "action" to Schema.enumeration(
                values = listOf("BUY", "SELL", "HOLD"),
                description = "Primary contract trade recommendation: BUY (Long), SELL (Short), or HOLD"
            ),
            "confidence" to Schema.integer(description = "Confidence score from 0 to 100"),
            "entryPrice" to Schema.double(description = "Recommended numeric entry price"),
            "stopLoss" to Schema.double(description = "Recommended numeric stop-loss price"),
            "takeProfit1" to Schema.double(description = "First numeric take-profit target"),
            "takeProfit2" to Schema.double(description = "Second numeric take-profit target"),
            "riskRewardRatio" to Schema.string(description = "Risk to reward ratio such as 1:2.2"),
            "suggestedLeverage" to Schema.string(description = "Conservative leverage range such as 2x - 5x"),
            "trendSummary" to Schema.string(description = "Concise 1-2 sentence Persian summary of market structure"),
            "reasoningBullets" to Schema.array(
                items = Schema.string(),
                description = "3 to 4 clear technical reasoning bullet points in Persian"
            ),
            "chartMarkers" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "candleIndex" to Schema.integer(description = "0-based candle index from the provided candle list"),
                        "action" to Schema.enumeration(values = listOf("BUY", "SELL")),
                        "price" to Schema.double(description = "Price level of the marker"),
                        "label" to Schema.string(description = "Short Persian/English tag like ورود خرید or مقاومت فروش")
                    )
                ),
                description = "2 to 4 key historical and current BUY/SELL inflection points on the provided candles"
            )
        ),
        optionalProperties = listOf("chartMarkers")
    )

    private val scannerSchema = Schema.obj(
        properties = mapOf(
            "signals" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "symbol" to Schema.string(description = "Pair symbol like BTCUSDT"),
                        "action" to Schema.enumeration(values = listOf("BUY", "SELL", "HOLD")),
                        "confidence" to Schema.integer(description = "Confidence score 0-100"),
                        "entryPrice" to Schema.double(description = "Numeric entry price"),
                        "stopLoss" to Schema.double(description = "Numeric stop loss price"),
                        "takeProfit" to Schema.double(description = "Numeric primary take profit price"),
                        "shortReason" to Schema.string(description = "One concise Persian sentence explaining the signal")
                    )
                ),
                description = "Ranked trading signals sorted by strongest actionable setups first"
            )
        )
    )

    private val signalModel by lazy {
        Firebase.ai.generativeModel(
            modelName = GEMINI_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = singleSignalSchema
                temperature = 0.25f
            }
        )
    }

    private val scannerModel by lazy {
        Firebase.ai.generativeModel(
            modelName = GEMINI_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = scannerSchema
                temperature = 0.2f
            }
        )
    }

    suspend fun analyzeLiveContractMarket(
        bundle: MarketBundle,
        timeframe: Timeframe
    ): AiTradingSignal {
        val candles = bundle.candles
        val ind = bundle.indicators
        val ticker = bundle.ticker
        val recentCount = minOf(36, candles.size)
        val startOffset = candles.size - recentCount

        val candleTable = buildString {
            appendLine("idx,open,high,low,close,vol")
            for (i in startOffset until candles.size) {
                val c = candles[i]
                appendLine(
                    String.format(
                        Locale.US,
                        "%d,%.6f,%.6f,%.6f,%.6f,%.2f",
                        i,
                        c.open,
                        c.high,
                        c.low,
                        c.close,
                        c.volume
                    )
                )
            }
        }

        val prompt = """
            You are an expert quantitative crypto perpetual-contract technical analyst assisting a trader connected to https://bc.game/trading/contract.
            Analyze the following LIVE market data and identify precise BUY (Long) and SELL (Short) points.
            All explanatory text (`trendSummary`, `reasoningBullets`, marker `label`) MUST be in natural, professional Persian (Farsi).
            Ensure Stop Loss and Take Profit levels are strictly consistent with the direction:
            - For BUY: stopLoss < entryPrice < takeProfit1 < takeProfit2
            - For SELL: takeProfit2 < takeProfit1 < entryPrice < stopLoss
            - For HOLD: provide the nearest breakout entryPrice, protective stopLoss, and takeProfit levels.
            For `chartMarkers`, choose 2 to 4 exact `candleIndex` values between $startOffset and ${candles.lastIndex} where key BUY (swing low / support bounce / bullish cross) and SELL (swing high / resistance rejection / bearish cross) points occurred, including the latest signal near index ${candles.lastIndex}.

            Pair: ${ticker.pair.displayPair} (Perpetual Contract)
            Timeframe: ${timeframe.label}
            Last Price: ${ticker.lastPrice}
            24h Change: ${String.format(Locale.US, "%.2f", ticker.changePercent24h)}%
            24h High / Low: ${ticker.high24h} / ${ticker.low24h}
            RSI(14): ${String.format(Locale.US, "%.2f", ind.rsi14)}
            EMA(9): ${ind.ema9} | EMA(21): ${ind.ema21} | EMA(50): ${ind.ema50}
            MACD: ${ind.macdLine} | Signal: ${ind.macdSignal} | Histogram: ${ind.macdHistogram}
            Bollinger Bands (20,2): Upper=${ind.bollingerUpper}, Mid=${ind.bollingerMiddle}, Lower=${ind.bollingerLower}
            ATR(14): ${ind.atr14}
            Recent Support: ${ind.supportLevel} | Resistance: ${ind.resistanceLevel}

            Recent OHLCV Candles (chronological):
            $candleTable
        """.trimIndent()

        val response = withTimeout(60_000L) {
            signalModel.generateContent(prompt)
        }
        val rawJson = response.text ?: throw IllegalStateException("پاسخ خالی از هوش مصنوعی دریافت شد.")
        return parseSingleSignalJson(
            rawJson = rawJson,
            fallbackPair = ticker.pair.displayPair,
            timeframeLabel = timeframe.label,
            maxCandleIndex = candles.lastIndex,
            isFromBcGameVision = false
        )
    }

    suspend fun analyzeBcGameTerminalScreenshot(
        screenshot: Bitmap,
        pageUrl: String,
        pageTitle: String,
        extractedPageText: String,
        fallbackPairDisplay: String
    ): AiTradingSignal {
        // Scale down bitmap to ensure lightweight payload well below 20MB limit.
        val scaledBitmap = scaleBitmapSafely(screenshot, maxDimension = 1280)
        val trimmedContext = extractedPageText.take(1500)

        val prompt = """
            You are an expert crypto contract trading analyst inspecting a live screenshot captured directly from the user's browser connected to https://bc.game/trading/contract.
            Inspect the chart, candlesticks, price action, trendlines, orderbook, or price display visible in the screenshot along with any page text below.
            Determine the clearest actionable BUY (Long), SELL (Short), or HOLD point.
            If a specific coin pair and live price are visible on the screen, use them for `pairDisplay`, `entryPrice`, `stopLoss`, `takeProfit1`, and `takeProfit2`.
            If the page is still loading, shows a login/region gate, or has no chart visible yet, set `action` to "HOLD", `confidence` to a low value (e.g. 20), explain clearly in Persian what is visible on the screen and ask the user to open a coin chart on bc.game/trading/contract before scanning again.
            Write `trendSummary` and `reasoningBullets` in fluent Persian (Farsi).

            Current URL: $pageUrl
            Page Title: $pageTitle
            Fallback Pair: $fallbackPairDisplay
            Visible Page Snippet:
            $trimmedContext
        """.trimIndent()

        val response = withTimeout(60_000L) {
            signalModel.generateContent(
                content {
                    image(scaledBitmap)
                    text(prompt)
                }
            )
        }
        val rawJson = response.text ?: throw IllegalStateException("پاسخ خالی از تحلیل تصویری دریافت شد.")
        return parseSingleSignalJson(
            rawJson = rawJson,
            fallbackPair = fallbackPairDisplay,
            timeframeLabel = "BC.Game Live",
            maxCandleIndex = -1,
            isFromBcGameVision = true
        )
    }

    suspend fun scanMarketBatch(
        bundles: List<MarketBundle>,
        timeframe: Timeframe
    ): List<ScannerSignalItem> {
        val summaryLines = buildString {
            bundles.forEach { b ->
                val t = b.ticker
                val ind = b.indicators
                val last3 = b.candles.takeLast(3).joinToString(" -> ") {
                    String.format(Locale.US, "%.4f", it.close)
                }
                appendLine(
                    "${t.pair.symbol} | price=${t.lastPrice} | chg24h=${String.format(Locale.US, "%.2f", t.changePercent24h)}% | " +
                        "RSI14=${String.format(Locale.US, "%.1f", ind.rsi14)} | EMA9=${ind.ema9} | EMA21=${ind.ema21} | " +
                        "MACD_H=${ind.macdHistogram} | Supp=${ind.supportLevel} | Res=${ind.resistanceLevel} | last3=[$last3]"
                )
            }
        }

        val prompt = """
            You are scanning multiple crypto perpetual contracts on timeframe ${timeframe.label} for a trader using https://bc.game/trading/contract.
            For EVERY symbol listed below, determine BUY, SELL, or HOLD, confidence (0-100), exact numeric entryPrice, stopLoss, takeProfit, and a sharp 1-sentence technical reason in Persian.
            Sort results so the highest-confidence BUY and SELL opportunities appear first.

            Live Market Snapshots:
            $summaryLines
        """.trimIndent()

        val response = withTimeout(60_000L) {
            scannerModel.generateContent(prompt)
        }
        val rawJson = response.text ?: throw IllegalStateException("پاسخ خالی از اسکنر هوشمند دریافت شد.")
        val root = JSONObject(rawJson)
        val array = root.optJSONArray("signals") ?: return emptyList()

        val bundleMap = bundles.associateBy { it.ticker.pair.symbol.uppercase(Locale.US) }
        val results = mutableListOf<ScannerSignalItem>()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val sym = obj.optString("symbol").uppercase(Locale.US).replace("/", "").replace("-", "")
            val bundle = bundleMap[sym] ?: continue
            val action = parseAction(obj.optString("action"))
            val confidence = obj.optInt("confidence", 50).coerceIn(0, 100)
            results.add(
                ScannerSignalItem(
                    pair = bundle.ticker.pair,
                    currentPrice = bundle.ticker.lastPrice,
                    changePercent24h = bundle.ticker.changePercent24h,
                    rsi14 = bundle.indicators.rsi14,
                    action = action,
                    confidence = confidence,
                    entryPrice = obj.optDouble("entryPrice", bundle.ticker.lastPrice),
                    stopLoss = obj.optDouble("stopLoss", bundle.indicators.supportLevel),
                    takeProfit = obj.optDouble("takeProfit", bundle.indicators.resistanceLevel),
                    shortReason = obj.optString("shortReason", "تحلیل تکنیکال انجام شد.")
                )
            )
        }
        return results.sortedByDescending {
            (if (it.action == SignalAction.HOLD) 0 else 100) + it.confidence
        }
    }

    private fun parseSingleSignalJson(
        rawJson: String,
        fallbackPair: String,
        timeframeLabel: String,
        maxCandleIndex: Int,
        isFromBcGameVision: Boolean
    ): AiTradingSignal {
        val json = JSONObject(rawJson)
        val action = parseAction(json.optString("action"))
        val confidence = json.optInt("confidence", 60).coerceIn(0, 100)
        val pairDisplay = json.optString("pairDisplay").ifBlank { fallbackPair }
        val entry = json.optDouble("entryPrice", 0.0)
        val sl = json.optDouble("stopLoss", 0.0)
        val tp1 = json.optDouble("takeProfit1", 0.0)
        val tp2 = json.optDouble("takeProfit2", 0.0)
        val rr = json.optString("riskRewardRatio", "1:2")
        val leverage = json.optString("suggestedLeverage", "2x - 5x")
        val trendSummary = json.optString("trendSummary", "")

        val bulletsArray = json.optJSONArray("reasoningBullets")
        val bullets = mutableListOf<String>()
        if (bulletsArray != null) {
            for (i in 0 until bulletsArray.length()) {
                val item = bulletsArray.optString(i)
                if (item.isNotBlank()) bullets.add(item)
            }
        }

        val markers = mutableListOf<ChartMarker>()
        val markersArray = json.optJSONArray("chartMarkers")
        if (markersArray != null && maxCandleIndex >= 0) {
            for (i in 0 until markersArray.length()) {
                val m = markersArray.optJSONObject(i) ?: continue
                val idx = m.optInt("candleIndex", -1)
                if (idx in 0..maxCandleIndex) {
                    markers.add(
                        ChartMarker(
                            candleIndex = idx,
                            action = parseAction(m.optString("action")),
                            price = m.optDouble("price", entry),
                            label = m.optString("label", "")
                        )
                    )
                }
            }
        }

        return AiTradingSignal(
            id = UUID.randomUUID().toString(),
            pairDisplay = pairDisplay,
            timeframeLabel = timeframeLabel,
            action = action,
            confidence = confidence,
            entryPrice = entry,
            stopLoss = sl,
            takeProfit1 = tp1,
            takeProfit2 = tp2,
            riskRewardRatio = rr,
            suggestedLeverage = leverage,
            trendSummary = trendSummary,
            reasoningBullets = bullets,
            chartMarkers = markers,
            isFromBcGameVision = isFromBcGameVision,
            analyzedAtMs = System.currentTimeMillis()
        )
    }

    private fun parseAction(raw: String): SignalAction {
        return when (raw.trim().uppercase(Locale.US)) {
            "BUY", "LONG" -> SignalAction.BUY
            "SELL", "SHORT" -> SignalAction.SELL
            else -> SignalAction.HOLD
        }
    }

    private fun scaleBitmapSafely(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap
        val ratio = width.toFloat() / height.toFloat()
        val (targetW, targetH) = if (width >= height) {
            maxDimension to (maxDimension / ratio).toInt().coerceAtLeast(1)
        } else {
            (maxDimension * ratio).toInt().coerceAtLeast(1) to maxDimension
        }
        return try {
            Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
        } catch (e: Exception) {
            Log.w(TAG, "Bitmap scaling fallback", e)
            bitmap
        }
    }
}

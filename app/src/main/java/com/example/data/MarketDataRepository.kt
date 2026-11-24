package com.example.data

import android.util.Log
import com.example.model.Candle
import com.example.model.CryptoPair
import com.example.model.TechnicalIndicators
import com.example.model.TickerSnapshot
import com.example.model.Timeframe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val TAG = "MarketDataRepository"

const val BC_GAME_CONTRACT_URL = "https://bc.game/trading/contract"

data class MarketBundle(
    val ticker: TickerSnapshot,
    val candles: List<Candle>,
    val indicators: TechnicalIndicators
)

class MarketDataRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {

    /**
     * Fetches live perpetual contract OHLCV candles and 24h ticker stats.
     * Primary source: OKX Perpetual Swap public feed (matching contract futures pricing).
     * Automatic fallback: Binance Data Vision public market API.
     */
    suspend fun fetchMarketBundle(
        pair: CryptoPair,
        timeframe: Timeframe,
        limit: Int = 72
    ): MarketBundle = withContext(Dispatchers.IO) {
        try {
            fetchFromOkxPerpetual(pair, timeframe, limit)
        } catch (okxError: Exception) {
            Log.w(TAG, "OKX perpetual fetch failed for ${pair.symbol}, trying Binance Vision", okxError)
            try {
                fetchFromBinanceVision(pair, timeframe, limit)
            } catch (binanceError: Exception) {
                Log.e(TAG, "All real market data feeds failed for ${pair.symbol}", binanceError)
                throw IOException(
                    "عدم دسترسی به دیتای زنده بازار برای ${pair.displayPair}. اتصال اینترنت را بررسی کنید.",
                    binanceError
                )
            }
        }
    }

    /**
     * Checks live HTTP reachability of https://bc.game/trading/contract
     */
    suspend fun checkBcGameReachability(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(BC_GAME_CONTRACT_URL)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                )
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                // Any HTTP response from bc.game (200, 301, 302, or Cloudflare challenge 403/503) confirms host reachability
                response.code in 200..503
            }
        } catch (e: Exception) {
            Log.w(TAG, "BC.Game reachability probe failed", e)
            false
        }
    }

    private fun fetchFromOkxPerpetual(
        pair: CryptoPair,
        timeframe: Timeframe,
        limit: Int
    ): MarketBundle {
        val instId = pair.okxSwapInstId
        val candlesUrl = "https://www.okx.com/api/v5/market/candles?instId=$instId&bar=${timeframe.okxBar}&limit=$limit"
        val tickerUrl = "https://www.okx.com/api/v5/market/ticker?instId=$instId"

        val candlesBody = executeGet(candlesUrl)
        val candlesRoot = JSONObject(candlesBody)
        if (candlesRoot.optString("code") != "0") {
            throw IOException("OKX candles error: ${candlesRoot.optString("msg")}")
        }
        val dataArray = candlesRoot.getJSONArray("data")
        val candles = ArrayList<Candle>(dataArray.length())
        for (i in 0 until dataArray.length()) {
            val row = dataArray.getJSONArray(i)
            candles.add(
                Candle(
                    openTimeMs = row.getString(0).toLong(),
                    open = row.getString(1).toDouble(),
                    high = row.getString(2).toDouble(),
                    low = row.getString(3).toDouble(),
                    close = row.getString(4).toDouble(),
                    volume = row.optString(5, "0").toDoubleOrNull() ?: 0.0
                )
            )
        }
        // OKX returns newest-first; sort chronologically oldest-first.
        candles.sortBy { it.openTimeMs }
        if (candles.isEmpty()) {
            throw IOException("Empty OKX candle dataset for $instId")
        }

        val tickerBody = executeGet(tickerUrl)
        val tickerRoot = JSONObject(tickerBody)
        val tickerData = tickerRoot.optJSONArray("data")?.optJSONObject(0)

        val lastPrice = tickerData?.optString("last")?.toDoubleOrNull() ?: candles.last().close
        val open24h = tickerData?.optString("open24h")?.toDoubleOrNull() ?: candles.first().open
        val changePct = if (open24h > 0.0) ((lastPrice - open24h) / open24h) * 100.0 else 0.0
        val high24h = tickerData?.optString("high24h")?.toDoubleOrNull() ?: candles.maxOf { it.high }
        val low24h = tickerData?.optString("low24h")?.toDoubleOrNull() ?: candles.minOf { it.low }
        val vol24h = tickerData?.optString("volCcy24h")?.toDoubleOrNull() ?: 0.0

        val ticker = TickerSnapshot(
            pair = pair,
            lastPrice = lastPrice,
            changePercent24h = changePct,
            high24h = high24h,
            low24h = low24h,
            volume24h = vol24h,
            feedName = "Perpetual Swap Feed"
        )
        val indicators = TechnicalIndicatorEngine.calculate(candles)
        return MarketBundle(ticker, candles, indicators)
    }

    private fun fetchFromBinanceVision(
        pair: CryptoPair,
        timeframe: Timeframe,
        limit: Int
    ): MarketBundle {
        val symbol = pair.symbol
        val candlesUrl = "https://data-api.binance.vision/api/v3/klines?symbol=$symbol&interval=${timeframe.binanceInterval}&limit=$limit"
        val tickerUrl = "https://data-api.binance.vision/api/v3/ticker/24hr?symbol=$symbol"

        val candlesBody = executeGet(candlesUrl)
        val arr = JSONArray(candlesBody)
        val candles = ArrayList<Candle>(arr.length())
        for (i in 0 until arr.length()) {
            val row = arr.getJSONArray(i)
            candles.add(
                Candle(
                    openTimeMs = row.getLong(0),
                    open = row.getString(1).toDouble(),
                    high = row.getString(2).toDouble(),
                    low = row.getString(3).toDouble(),
                    close = row.getString(4).toDouble(),
                    volume = row.getString(5).toDouble()
                )
            )
        }
        if (candles.isEmpty()) {
            throw IOException("Empty Binance candle dataset for $symbol")
        }

        val tickerBody = executeGet(tickerUrl)
        val obj = JSONObject(tickerBody)
        val ticker = TickerSnapshot(
            pair = pair,
            lastPrice = obj.optString("lastPrice").toDoubleOrNull() ?: candles.last().close,
            changePercent24h = obj.optString("priceChangePercent").toDoubleOrNull() ?: 0.0,
            high24h = obj.optString("highPrice").toDoubleOrNull() ?: candles.maxOf { it.high },
            low24h = obj.optString("lowPrice").toDoubleOrNull() ?: candles.minOf { it.low },
            volume24h = obj.optString("quoteVolume").toDoubleOrNull() ?: 0.0,
            feedName = "Spot/Contract Index"
        )
        val indicators = TechnicalIndicatorEngine.calculate(candles)
        return MarketBundle(ticker, candles, indicators)
    }

    private fun executeGet(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code} for $url")
            }
            return response.body?.string() ?: throw IOException("Empty response body for $url")
        }
    }
}

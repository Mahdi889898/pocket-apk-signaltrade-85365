package com.example.data

import com.example.model.Candle
import com.example.model.TechnicalIndicators
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

object TechnicalIndicatorEngine {

    fun calculate(candles: List<Candle>): TechnicalIndicators {
        if (candles.isEmpty()) {
            return TechnicalIndicators(
                rsi14 = 50.0,
                ema9 = 0.0,
                ema21 = 0.0,
                ema50 = 0.0,
                macdLine = 0.0,
                macdSignal = 0.0,
                macdHistogram = 0.0,
                bollingerUpper = 0.0,
                bollingerMiddle = 0.0,
                bollingerLower = 0.0,
                atr14 = 0.0,
                supportLevel = 0.0,
                resistanceLevel = 0.0,
                ema9Series = emptyList(),
                ema21Series = emptyList()
            )
        }

        val closes = candles.map { it.close }
        val ema9Series = computeEmaSeries(closes, 9)
        val ema21Series = computeEmaSeries(closes, 21)
        val ema50Series = computeEmaSeries(closes, 50)
        val ema12Series = computeEmaSeries(closes, 12)
        val ema26Series = computeEmaSeries(closes, 26)

        val macdSeries = ema12Series.zip(ema26Series) { fast, slow -> fast - slow }
        val signalSeries = computeEmaSeries(macdSeries, 9)
        val macdLine = macdSeries.lastOrNull() ?: 0.0
        val macdSignal = signalSeries.lastOrNull() ?: 0.0
        val macdHist = macdLine - macdSignal

        val rsi14 = computeRsi(closes, 14)
        val (bbUpper, bbMiddle, bbLower) = computeBollingerBands(closes, 20, 2.0)
        val atr14 = computeAtr(candles, 14)

        val recentWindow = candles.takeLast(minOf(30, candles.size))
        val support = recentWindow.minOf { it.low }
        val resistance = recentWindow.maxOf { it.high }

        return TechnicalIndicators(
            rsi14 = rsi14,
            ema9 = ema9Series.lastOrNull() ?: closes.last(),
            ema21 = ema21Series.lastOrNull() ?: closes.last(),
            ema50 = ema50Series.lastOrNull() ?: closes.last(),
            macdLine = macdLine,
            macdSignal = macdSignal,
            macdHistogram = macdHist,
            bollingerUpper = bbUpper,
            bollingerMiddle = bbMiddle,
            bollingerLower = bbLower,
            atr14 = atr14,
            supportLevel = support,
            resistanceLevel = resistance,
            ema9Series = ema9Series,
            ema21Series = ema21Series
        )
    }

    private fun computeEmaSeries(values: List<Double>, period: Int): List<Double> {
        if (values.isEmpty()) return emptyList()
        val k = 2.0 / (period + 1.0)
        val result = ArrayList<Double>(values.size)
        var ema = values.first()
        result.add(ema)
        for (i in 1 until values.size) {
            ema = values[i] * k + ema * (1.0 - k)
            result.add(ema)
        }
        return result
    }

    private fun computeRsi(closes: List<Double>, period: Int): Double {
        if (closes.size <= period) return 50.0
        var gainSum = 0.0
        var lossSum = 0.0
        for (i in 1..period) {
            val diff = closes[i] - closes[i - 1]
            if (diff >= 0) gainSum += diff else lossSum += -diff
        }
        var avgGain = gainSum / period
        var avgLoss = lossSum / period

        for (i in (period + 1) until closes.size) {
            val diff = closes[i] - closes[i - 1]
            val gain = if (diff > 0) diff else 0.0
            val loss = if (diff < 0) -diff else 0.0
            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period
        }

        if (avgLoss == 0.0) return 100.0
        val rs = avgGain / avgLoss
        return 100.0 - (100.0 / (1.0 + rs))
    }

    private fun computeBollingerBands(
        closes: List<Double>,
        period: Int,
        stdDevMultiplier: Double
    ): Triple<Double, Double, Double> {
        val window = closes.takeLast(minOf(period, closes.size))
        val mean = window.average()
        val variance = window.map { (it - mean) * (it - mean) }.average()
        val stdDev = sqrt(variance)
        return Triple(
            mean + stdDevMultiplier * stdDev,
            mean,
            mean - stdDevMultiplier * stdDev
        )
    }

    private fun computeAtr(candles: List<Candle>, period: Int): Double {
        if (candles.size < 2) return 0.0
        val trs = ArrayList<Double>(candles.size - 1)
        for (i in 1 until candles.size) {
            val current = candles[i]
            val prevClose = candles[i - 1].close
            val tr = max(
                current.high - current.low,
                max(abs(current.high - prevClose), abs(current.low - prevClose))
            )
            trs.add(tr)
        }
        val window = trs.takeLast(minOf(period, trs.size))
        return if (window.isEmpty()) 0.0 else window.average()
    }
}

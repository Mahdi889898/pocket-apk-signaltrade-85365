package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AiTradingSignal
import com.example.model.Candle
import com.example.model.SignalAction
import com.example.model.TechnicalIndicators
import com.example.model.formatCryptoPrice
import com.example.ui.theme.AiCyan
import com.example.ui.theme.BuyEmerald
import com.example.ui.theme.BuyEmeraldBright
import com.example.ui.theme.ChartEmaFast
import com.example.ui.theme.ChartEmaSlow
import com.example.ui.theme.ChartGridLine
import com.example.ui.theme.ChartVolumeBear
import com.example.ui.theme.ChartVolumeBull
import com.example.ui.theme.MonoTextStyles
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.SellCoral
import com.example.ui.theme.SellCoralBright
import com.example.ui.theme.TextSecondaryDark
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun InteractiveCandlestickChart(
    candles: List<Candle>,
    indicators: TechnicalIndicators,
    activeSignal: AiTradingSignal?,
    modifier: Modifier = Modifier
) {
    if (candles.isEmpty()) return

    // Display the latest 48 candles for crisp visual readability
    val displayCount = min(48, candles.size)
    val startOffset = candles.size - displayCount
    val visibleCandles = remember(candles) { candles.subList(startOffset, candles.size) }
    val visibleEma9 = remember(indicators.ema9Series, startOffset) {
        if (indicators.ema9Series.size >= candles.size) {
            indicators.ema9Series.subList(startOffset, candles.size)
        } else {
            emptyList()
        }
    }
    val visibleEma21 = remember(indicators.ema21Series, startOffset) {
        if (indicators.ema21Series.size >= candles.size) {
            indicators.ema21Series.subList(startOffset, candles.size)
        } else {
            emptyList()
        }
    }

    var selectedLocalIndex by remember(candles) { mutableIntStateOf(-1) }
    val inspectedCandle = if (selectedLocalIndex in visibleCandles.indices) {
        visibleCandles[selectedLocalIndex]
    } else {
        visibleCandles.last()
    }

    val priceBounds = remember(visibleCandles, activeSignal) {
        var minP = visibleCandles.minOf { it.low }
        var maxP = visibleCandles.maxOf { it.high }
        activeSignal?.let { sig ->
            listOf(sig.entryPrice, sig.stopLoss, sig.takeProfit1)
                .filter { it > 0.0 && it.isFinite() }
                .forEach { level ->
                    // Keep price bounds sensible if signal levels are near current price
                    if (level in (minP * 0.8)..(maxP * 1.2)) {
                        minP = min(minP, level)
                        maxP = max(maxP, level)
                    }
                }
        }
        val pad = (maxP - minP).coerceAtLeast(maxP * 0.002) * 0.14
        (minP - pad) to (maxP + pad)
    }

    val maxVolume = remember(visibleCandles) {
        visibleCandles.maxOfOrNull { it.volume }?.coerceAtLeast(1.0) ?: 1.0
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(ObsidianSurfaceElevated, ObsidianSurface)
                    )
                )
                .border(1.dp, ObsidianBorder, RoundedCornerShape(20.dp))
                .padding(12.dp)
                .testTag("candlestick_chart_card")
        ) {
            // OHLCV Strip + EMA Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OhlcTag("O", inspectedCandle.open, TextSecondaryDark)
                    OhlcTag("H", inspectedCandle.high, BuyEmeraldBright)
                    OhlcTag("L", inspectedCandle.low, SellCoralBright)
                    OhlcTag(
                        "C",
                        inspectedCandle.close,
                        if (inspectedCandle.isBullish) BuyEmeraldBright else SellCoralBright
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendDot("EMA9", ChartEmaFast)
                    LegendDot("EMA21", ChartEmaSlow)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            ) {
                val (minPrice, maxPrice) = priceBounds
                val priceRange = (maxPrice - minPrice).coerceAtLeast(1e-8)

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 68.dp, top = 8.dp, bottom = 8.dp)
                        .pointerInput(visibleCandles) {
                            detectTapGestures { offset ->
                                val step = size.width / visibleCandles.size.toFloat()
                                val idx = (offset.x / step).toInt().coerceIn(0, visibleCandles.lastIndex)
                                selectedLocalIndex = if (selectedLocalIndex == idx) -1 else idx
                            }
                        }
                        .pointerInput(visibleCandles) {
                            detectHorizontalDragGestures(
                                onDragEnd = { selectedLocalIndex = -1 },
                                onDragCancel = { selectedLocalIndex = -1 }
                            ) { change, _ ->
                                val step = size.width / visibleCandles.size.toFloat()
                                selectedLocalIndex =
                                    (change.position.x / step).toInt().coerceIn(0, visibleCandles.lastIndex)
                            }
                        }
                ) {
                    val chartHeight = size.height
                    val chartWidth = size.width
                    val candleSlot = chartWidth / visibleCandles.size.toFloat()
                    val bodyWidth = (candleSlot * 0.62f).coerceIn(3f, 16f)

                    fun priceToY(price: Double): Float {
                        val ratio = ((price - minPrice) / priceRange).toFloat().coerceIn(0f, 1f)
                        return chartHeight * (1f - ratio)
                    }

                    // 1. Subtle horizontal gridlines
                    val dashGrid = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    for (g in 0..4) {
                        val y = chartHeight * (g / 4f)
                        drawLine(
                            color = ChartGridLine,
                            start = Offset(0f, y),
                            end = Offset(chartWidth, y),
                            strokeWidth = 1f,
                            pathEffect = dashGrid
                        )
                    }

                    // 2. Volume bars at bottom 22%
                    val volMaxHeight = chartHeight * 0.20f
                    visibleCandles.forEachIndexed { idx, candle ->
                        val cx = idx * candleSlot + candleSlot / 2f
                        val volH = ((candle.volume / maxVolume) * volMaxHeight).toFloat()
                        drawRect(
                            color = if (candle.isBullish) ChartVolumeBull else ChartVolumeBear,
                            topLeft = Offset(cx - bodyWidth / 2f, chartHeight - volH),
                            size = Size(bodyWidth, volH)
                        )
                    }

                    // 3. Candlesticks (wicks + bodies)
                    visibleCandles.forEachIndexed { idx, candle ->
                        val cx = idx * candleSlot + candleSlot / 2f
                        val highY = priceToY(candle.high)
                        val lowY = priceToY(candle.low)
                        val openY = priceToY(candle.open)
                        val closeY = priceToY(candle.close)
                        val color = if (candle.isBullish) BuyEmerald else SellCoral

                        // Wick
                        drawLine(
                            color = color,
                            start = Offset(cx, highY),
                            end = Offset(cx, lowY),
                            strokeWidth = 2f,
                            cap = StrokeCap.Round
                        )

                        // Body
                        val topY = min(openY, closeY)
                        val bottomY = max(openY, closeY)
                        val bodyH = (bottomY - topY).coerceAtLeast(2.5f)
                        drawRect(
                            color = color,
                            topLeft = Offset(cx - bodyWidth / 2f, topY),
                            size = Size(bodyWidth, bodyH)
                        )
                    }

                    // 4. EMA 9 & EMA 21 smooth lines
                    fun drawLineSeries(series: List<Double>, strokeColor: Color) {
                        if (series.size < 2) return
                        val path = Path()
                        series.forEachIndexed { idx, value ->
                            val x = idx * candleSlot + candleSlot / 2f
                            val y = priceToY(value)
                            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(
                            path = path,
                            color = strokeColor,
                            style = Stroke(width = 2.4f, cap = StrokeCap.Round)
                        )
                    }
                    drawLineSeries(visibleEma9, ChartEmaFast)
                    drawLineSeries(visibleEma21, ChartEmaSlow)

                    // 5. Active AI Signal horizontal levels (Entry, SL, TP1)
                    if (activeSignal != null) {
                        val levelDash = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                        if (activeSignal.takeProfit1 in minPrice..maxPrice) {
                            val tpY = priceToY(activeSignal.takeProfit1)
                            drawLine(
                                color = BuyEmeraldBright,
                                start = Offset(0f, tpY),
                                end = Offset(chartWidth, tpY),
                                strokeWidth = 2f,
                                pathEffect = levelDash
                            )
                        }
                        if (activeSignal.entryPrice in minPrice..maxPrice) {
                            val entryY = priceToY(activeSignal.entryPrice)
                            drawLine(
                                color = AiCyan,
                                start = Offset(0f, entryY),
                                end = Offset(chartWidth, entryY),
                                strokeWidth = 2.2f,
                                pathEffect = levelDash
                            )
                        }
                        if (activeSignal.stopLoss in minPrice..maxPrice) {
                            val slY = priceToY(activeSignal.stopLoss)
                            drawLine(
                                color = SellCoralBright,
                                start = Offset(0f, slY),
                                end = Offset(chartWidth, slY),
                                strokeWidth = 2f,
                                pathEffect = levelDash
                            )
                        }

                        // 6. Plot AI BUY (▲) and SELL (▼) glowing markers on candles
                        activeSignal.chartMarkers.forEach { marker ->
                            val localIdx = marker.candleIndex - startOffset
                            if (localIdx in visibleCandles.indices) {
                                val candle = visibleCandles[localIdx]
                                val cx = localIdx * candleSlot + candleSlot / 2f
                                val isBuy = marker.action == SignalAction.BUY
                                val markerColor = if (isBuy) BuyEmeraldBright else SellCoralBright
                                val anchorY = if (isBuy) {
                                    (priceToY(candle.low) + 16f).coerceAtMost(chartHeight - 10f)
                                } else {
                                    (priceToY(candle.high) - 16f).coerceAtLeast(10f)
                                }

                                // Halo glow
                                drawCircle(
                                    color = markerColor.copy(alpha = 0.28f),
                                    radius = 15f,
                                    center = Offset(cx, anchorY)
                                )
                                drawCircle(
                                    color = markerColor,
                                    radius = 9f,
                                    center = Offset(cx, anchorY)
                                )

                                // Directional triangle glyph inside marker
                                val triPath = Path()
                                if (isBuy) {
                                    triPath.moveTo(cx, anchorY - 4.5f)
                                    triPath.lineTo(cx - 4.5f, anchorY + 3.5f)
                                    triPath.lineTo(cx + 4.5f, anchorY + 3.5f)
                                    triPath.close()
                                } else {
                                    triPath.moveTo(cx, anchorY + 4.5f)
                                    triPath.lineTo(cx - 4.5f, anchorY - 3.5f)
                                    triPath.lineTo(cx + 4.5f, anchorY - 3.5f)
                                    triPath.close()
                                }
                                drawPath(path = triPath, color = Color(0xFF070B14))
                            }
                        }
                    }

                    // 7. Live last close horizontal reference line
                    val lastCloseY = priceToY(visibleCandles.last().close)
                    drawLine(
                        color = Color.White.copy(alpha = 0.35f),
                        start = Offset(0f, lastCloseY),
                        end = Offset(chartWidth, lastCloseY),
                        strokeWidth = 1.2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                    )

                    // 8. Interactive Crosshair
                    if (selectedLocalIndex in visibleCandles.indices) {
                        val cx = selectedLocalIndex * candleSlot + candleSlot / 2f
                        val cy = priceToY(visibleCandles[selectedLocalIndex].close)
                        drawLine(
                            color = AiCyan.copy(alpha = 0.6f),
                            start = Offset(cx, 0f),
                            end = Offset(cx, chartHeight),
                            strokeWidth = 1.5f
                        )
                        drawCircle(
                            color = AiCyan,
                            radius = 5f,
                            center = Offset(cx, cy)
                        )
                    }
                }

                // Right-side Monospace Price Axis
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(66.dp)
                        .fillMaxSize()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    for (g in 0..4) {
                        val price = maxPrice - (priceRange * (g / 4.0))
                        Text(
                            text = formatCompactAxisPrice(price),
                            style = MonoTextStyles.microAxis,
                            color = TextSecondaryDark,
                            maxLines = 1
                        )
                    }
                }
            }

            // Signal Chart Key Legend when AI markers exist
            if (activeSignal != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ObsidianSurface.copy(alpha = 0.8f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendDot("▲ BUY Point", BuyEmeraldBright)
                    LegendDot("▼ SELL Point", SellCoralBright)
                    LegendDot("Entry", AiCyan)
                    LegendDot("TP / SL", ChartEmaSlow)
                }
            }
        }
    }
}

@Composable
private fun OhlcTag(label: String, value: Double, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label:",
            style = MonoTextStyles.microAxis,
            color = TextSecondaryDark
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = formatCompactAxisPrice(value),
            style = MonoTextStyles.microAxis.copy(fontWeight = FontWeight.SemiBold),
            color = color
        )
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = TextSecondaryDark
        )
    }
}

private fun formatCompactAxisPrice(price: Double): String {
    if (!price.isFinite() || price <= 0.0) return "—"
    return when {
        price >= 1000.0 -> String.format(java.util.Locale.US, "%.1f", price)
        price >= 1.0 -> String.format(java.util.Locale.US, "%.3f", price)
        price >= 0.01 -> String.format(java.util.Locale.US, "%.4f", price)
        else -> formatCryptoPrice(price).removePrefix("$")
    }
}

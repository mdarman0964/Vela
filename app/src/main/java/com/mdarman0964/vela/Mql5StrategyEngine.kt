package com.mdarman0964.vela

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

/** Read-only Kotlin port of the VolumeProfile_Bounce_Strategy_v3.mq5 calculations. */
data class MarketCandle(
    val timeMillis: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val tickVolume: Long,
    val bid: Double = close,
    val ask: Double = close,
) {
    init {
        require(high >= max(open, close) && low <= min(open, close))
        require(tickVolume >= 0)
    }
}

data class VolumeProfile(val poc: Double, val vah: Double, val valPrice: Double, val totalVolume: Double)

data class StrategySignal(
    val timeframe: Timeframe,
    val profile: VolumeProfile?,
    val sma: Double?,
    val atr: Double?,
    val midPrice: Double,
    val buy: Boolean,
    val sell: Boolean,
    val reason: String,
)

object Mql5StrategyEngine {
    fun volumeProfile(
        candles: List<MarketCandle>,
        lookback: Int = 200,
        bins: Int = 24,
        valueAreaPercent: Double = 70.0,
    ): VolumeProfile? {
        require(lookback >= 10 && bins >= 3)
        require(valueAreaPercent > 0.0 && valueAreaPercent <= 100.0)
        if (candles.size < lookback + 1) return null
        val history = candles.takeLast(lookback + 1).dropLast(1)
        val highest = history.maxOf { it.high }
        val lowest = history.minOf { it.low }
        if (highest <= lowest) return null
        val binSize = (highest - lowest) / bins
        val volumes = DoubleArray(bins)
        history.forEach { candle ->
            val first = floor((candle.low - lowest) / binSize).toInt().coerceIn(0, bins - 1)
            val last = floor((candle.high - lowest) / binSize).toInt().coerceIn(0, bins - 1)
            val span = last - first + 1
            val perBin = candle.tickVolume.toDouble() / span
            for (b in first..last) volumes[b] += perBin
        }
        val total = volumes.sum()
        if (total <= 0.0) return null
        var pocBin = 0
        var maxVolume = -1.0
        volumes.forEachIndexed { index, volume ->
            if (volume > maxVolume) {
                maxVolume = volume
                pocBin = index
            }
        }
        val target = total * valueAreaPercent / 100.0
        var covered = volumes[pocBin]
        var lowBin = pocBin
        var highBin = pocBin
        while (covered < target && (lowBin > 0 || highBin < bins - 1)) {
            val below = if (lowBin > 0) volumes[lowBin - 1] else -1.0
            val above = if (highBin < bins - 1) volumes[highBin + 1] else -1.0
            if (above >= below && highBin < bins - 1) {
                highBin++
                covered += volumes[highBin]
            } else if (lowBin > 0) {
                lowBin--
                covered += volumes[lowBin]
            } else break
        }
        return VolumeProfile(
            poc = lowest + (pocBin + 0.5) * binSize,
            valPrice = lowest + lowBin * binSize,
            vah = lowest + (highBin + 1) * binSize,
            totalVolume = total,
        )
    }

    fun sma(candles: List<MarketCandle>, period: Int = 50): Double? {
        require(period > 0)
        if (candles.size < period) return null
        return candles.takeLast(period).map { it.close }.average()
    }

    /** Wilder-smoothed ATR, matching the standard MT5 iATR indicator rather than a simple TR average. */
    fun atr(candles: List<MarketCandle>, period: Int = 14): Double? {
        require(period > 0)
        if (candles.size < period + 1) return null
        fun trueRange(index: Int): Double {
            val current = candles[index]
            val previousClose = candles[index - 1].close
            return max(current.high - current.low, max(abs(current.high - previousClose), abs(current.low - previousClose)))
        }
        var value = (1..period).map(::trueRange).average()
        for (index in (period + 1) until candles.size) {
            value = (value * (period - 1) + trueRange(index)) / period
        }
        return value
    }

    /** Evaluate on the newly opened bar; profile uses the previous closed bars, like MQL5 shift 1..N. */
    fun evaluate(
        timeframe: Timeframe,
        candles: List<MarketCandle>,
        point: Double,
        defaults: StrategyDefaults = StrategyDefaults(),
        useSmaFilter: Boolean = true,
    ): StrategySignal {
        require(point > 0.0)
        val profile = volumeProfile(candles, defaults.vpLookbackBars, defaults.vpNumBins, defaults.vpValueAreaPercent)
        val sma = if (useSmaFilter) sma(candles, defaults.smaPeriod) else null
        val atr = atr(candles, defaults.atrPeriod)
        val current = candles.lastOrNull()
        val mid = current?.let { (it.bid + it.ask) / 2.0 } ?: 0.0
        if (current == null || profile == null || (useSmaFilter && sma == null)) {
            return StrategySignal(timeframe, profile, sma, atr, mid, false, false, "Insufficient candle history")
        }
        val tolerance = defaults.vpBounceTolerancePoints * point
        val nearVal = abs(mid - profile.valPrice) <= tolerance
        val nearPoc = abs(mid - profile.poc) <= tolerance
        val nearVah = abs(mid - profile.vah) <= tolerance
        val trendUp = !useSmaFilter || mid > sma!!
        val trendDown = !useSmaFilter || mid < sma!!
        val buy = trendUp && (nearVal || (nearPoc && mid >= profile.poc))
        val sell = !buy && trendDown && (nearVah || (nearPoc && mid <= profile.poc))
        val reason = when {
            buy -> "BUY setup: bullish SMA alignment and VAL/POC proximity"
            sell -> "SELL setup: bearish SMA alignment and VAH/POC proximity"
            else -> "No entry: bounce and trend conditions not both met"
        }
        return StrategySignal(timeframe, profile, sma, atr, mid, buy, sell, reason)
    }

    fun initialStops(entry: Double, side: Side, point: Double, defaults: StrategyDefaults = StrategyDefaults()): Pair<Double, Double> {
        require(entry > 0.0 && point > 0.0)
        val direction = if (side == Side.BUY) 1.0 else -1.0
        val sl = normalizePrice(entry - direction * defaults.fixedStopLossPoints * point, point)
        val tp = normalizePrice(entry + direction * defaults.fixedTakeProfitPoints * point, point)
        return sl to tp
    }

    private fun normalizePrice(price: Double, point: Double): Double = round(price / point) * point

    fun trailingDistance(atr: Double, spread: Double, point: Double, defaults: StrategyDefaults = StrategyDefaults()): Double {
        require(atr >= 0.0 && spread >= 0.0 && point > 0.0)
        val atrPoints = (atr / point) * defaults.trailAtrMultiplier
        val spreadPoints = (spread / point) * defaults.spreadSafetyMultiplier
        return max(max(atrPoints, spreadPoints), defaults.trailMinimumPoints.toDouble()) * point
    }

    /** Pure port of the EA's tick-level trailing-stop ratchet; null means no update. */
    fun nextTrailingStop(
        side: Side,
        openPrice: Double,
        currentStop: Double,
        currentBid: Double,
        currentAsk: Double,
        atr: Double,
        point: Double,
        defaults: StrategyDefaults = StrategyDefaults(),
    ): Double? {
        require(openPrice > 0.0 && currentBid > 0.0 && currentAsk >= currentBid && point > 0.0)
        if (atr <= 0.0) return null
        val startDistance = atr * defaults.trailStartAtrMultiplier
        val distance = trailingDistance(atr, currentAsk - currentBid, point, defaults)
        return when (side) {
            Side.BUY -> {
                if (currentBid - openPrice < startDistance) null
                else {
                    val next = normalizePrice(currentBid - distance, point)
                    if ((currentStop == 0.0 || next > currentStop) && next < currentBid) next else null
                }
            }
            Side.SELL -> {
                if (openPrice - currentAsk < startDistance) null
                else {
                    val next = normalizePrice(currentAsk + distance, point)
                    if ((currentStop == 0.0 || next < currentStop) && next > currentAsk) next else null
                }
            }
        }
    }
}

enum class Side { BUY, SELL }

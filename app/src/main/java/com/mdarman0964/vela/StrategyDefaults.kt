package com.mdarman0964.vela

/**
 * Defaults transcribed from VolumeProfile_Bounce_Strategy_v3.mq5.
 * Keep these independent of the selected chart timeframe unless the original EA
 * explicitly defines a timeframe-dependent parameter.
 */
data class StrategyDefaults(
    val lotSize: Double = 0.01,
    val magicNumber: Long = 20260918L,
    val slippagePoints: Int = 10,
    val vpLookbackBars: Int = 200,
    val vpNumBins: Int = 24,
    val vpValueAreaPercent: Double = 70.0,
    val vpBounceTolerancePoints: Int = 30,
    val smaPeriod: Int = 50,
    val fixedStopLossPoints: Int = 300,
    val fixedTakeProfitPoints: Int = 600,
    val atrPeriod: Int = 14,
    val trailAtrMultiplier: Double = 0.05,
    val trailStartAtrMultiplier: Double = 1.0,
    val spreadSafetyMultiplier: Double = 1.5,
    val trailMinimumPoints: Int = 50,
)

enum class TradingMode { OBSERVE_ONLY, LIVE_DISABLED }

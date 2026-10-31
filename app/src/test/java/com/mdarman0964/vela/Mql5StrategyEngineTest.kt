package com.mdarman0964.vela

import org.junit.Assert.*
import org.junit.Test

class Mql5StrategyEngineTest {
    private fun candles(intervalMs: Long, count: Int = 220): List<MarketCandle> =
        (0 until count).map { i ->
            val base = 4000.0 + (i % 17) * 0.12
            MarketCandle(i * intervalMs, base, base + 1.2, base - 0.8, base + 0.25,
                100L + (i % 9) * 10L, base + 0.20, base + 0.30)
        }

    @Test fun volumeProfileReturnsOrderedLevelsFromPreviousClosedBars() {
        val profile = Mql5StrategyEngine.volumeProfile(candles(Timeframe.M5.durationMillis))
        assertNotNull(profile)
        assertTrue(profile!!.valPrice <= profile.poc)
        assertTrue(profile.poc <= profile.vah)
        assertTrue(profile.totalVolume > 0)
    }

    @Test fun strategyEvaluatesAllFourRequestedTimeframes() {
        listOf(
            Timeframe.M1 to 60_000L, Timeframe.M5 to 300_000L,
            Timeframe.M15 to 900_000L, Timeframe.H1 to 3_600_000L
        ).forEach { (timeframe, interval) ->
            val signal = Mql5StrategyEngine.evaluate(timeframe, candles(interval), 0.01)
            assertEquals(timeframe, signal.timeframe)
            assertNotNull("Profile should warm up for $timeframe", signal.profile)
            assertNotNull("SMA should warm up for $timeframe", signal.sma)
            assertNotNull("ATR should warm up for $timeframe", signal.atr)
            assertFalse(signal.buy && signal.sell)
        }
    }

    @Test fun insufficientHistoryNeverCreatesSignal() {
        val signal = Mql5StrategyEngine.evaluate(Timeframe.M5, candles(Timeframe.M5.durationMillis, 20), 0.01)
        assertNull(signal.profile)
        assertFalse(signal.buy)
        assertFalse(signal.sell)
    }

    @Test fun initialStopsMatchMql5PointDistances() {
        val (sl, tp) = Mql5StrategyEngine.initialStops(4000.0, Side.BUY, 0.01)
        assertEquals(3997.0, sl, 1e-9)
        assertEquals(4006.0, tp, 1e-9)
    }

    @Test fun trailingDistanceUsesLargestAtrSpreadOrMinimumFloor() {
        assertEquals(0.5, Mql5StrategyEngine.trailingDistance(2.0, 0.4, 0.01), 1e-9)
    }
}

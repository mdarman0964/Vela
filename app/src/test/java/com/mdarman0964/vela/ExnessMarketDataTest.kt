package com.mdarman0964.vela

import org.junit.Assert.assertTrue
import org.junit.Test

class ExnessMarketDataTest {
    @Test fun requestedTimeframeIsIncludedInSignedPath() {
        val path = ExnessMarketData.candlesPath("123", "XAUUSDm", Timeframe.M15, "2026-10-31T10:00:00Z", 500)
        assertTrue(path.contains("&timeframe=M15&"))
    }

    @Test fun supportsAllRequestedIntervals() {
        val expected = listOf("M1", "M5", "M15", "H1")
        val actual = Timeframe.entries.map {
            ExnessMarketData.candlesPath("123", "XAUUSDm", it, "2026-10-31T10:00:00Z", 100)
                .substringAfter("&timeframe=").substringBefore("&")
        }
        assertTrue(actual == expected)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsBlankAccountId() {
        ExnessMarketData.candlesPath("", "XAUUSDm", Timeframe.M5, "2026-10-31T10:00:00Z", 100)
    }
    @Test fun requestsBothSidesAndPreservesEncodedSignedQuery() {
        val path = ExnessMarketData.candlesPath("123", "XAUUSDm", Timeframe.H1, "2026-11-01T10:30:00Z", 300)
        assertTrue(path.contains("price_type=both"))
        assertTrue(path.contains("timeframe=H1"))
        assertTrue(path.contains("from=2026-11-01T10%3A30%3A00Z"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsCandleCountAboveSafeApiBound() {
        ExnessMarketData.candlesPath("123", "XAUUSDm", Timeframe.M5, "2026-11-01T10:00:00Z", 1001)
    }

}

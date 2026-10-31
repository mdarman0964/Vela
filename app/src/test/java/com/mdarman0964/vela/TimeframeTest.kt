package com.mdarman0964.vela

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeframeTest {
    @Test fun exposesRequestedIntervalsInOrder() {
        assertEquals(listOf("1M", "5M", "15M", "1H"), Timeframe.entries.map { it.label })
    }

    @Test fun defaultsToFiveMinutes() {
        assertEquals(Timeframe.M5, Timeframe.default)
    }

    @Test fun mapsToExnessTimeframeCodes() {
        assertEquals(listOf("M1", "M5", "M15", "H1"), Timeframe.entries.map { it.apiValue })
    }

    @Test fun unknownApiTimeframeFallsBackToDefault() {
        assertEquals(Timeframe.M5, Timeframe.fromApiValue("M30"))
    }
}

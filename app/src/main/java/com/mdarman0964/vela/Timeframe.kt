package com.mdarman0964.vela

enum class Timeframe(val label: String, val apiValue: String, val durationMillis: Long) {
    M1("1M", "M1", 60_000L),
    M5("5M", "M5", 300_000L),
    M15("15M", "M15", 900_000L),
    H1("1H", "H1", 3_600_000L);

    companion object {
        val default = M5
        fun fromApiValue(value: String): Timeframe = entries.firstOrNull { it.apiValue == value } ?: default
    }
}

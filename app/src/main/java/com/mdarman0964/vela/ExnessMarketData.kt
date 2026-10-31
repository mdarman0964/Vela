package com.mdarman0964.vela

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Constructs the exact candle resource path used by Exness Trader API.
 * The caller must sign this exact path including its query string before sending.
 */
object ExnessMarketData {
    fun candlesPath(
        accountId: String,
        instrument: String,
        timeframe: Timeframe,
        fromIso8601: String,
        count: Int,
    ): String {
        require(Regex("^[0-9]{1,20}$").matches(accountId)) { "Account ID must contain 1 to 20 digits." }
        require(Regex("^[A-Za-z0-9._-]{1,11}$").matches(instrument)) { "Instrument must be 1 to 11 supported characters." }
        require(count in 1..1000) { "Candle count must be between 1 and 1000; obey the configured API limit." }
        val encodedInstrument = encode(instrument)
        val encodedFrom = encode(fromIso8601)
        return "/v1/market-data/accounts/$accountId/candles" +
            "?instrument=$encodedInstrument" +
            "&price_type=bid" +
            "&timeframe=${timeframe.apiValue}" +
            "&from=$encodedFrom" +
            "&count=$count"
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")
}

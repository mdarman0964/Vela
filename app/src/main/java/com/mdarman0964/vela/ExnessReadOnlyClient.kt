package com.mdarman0964.vela

import android.util.Base64
import okhttp3.OkHttpClient
import okhttp3.Request
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.concurrent.TimeUnit

data class ExnessCandle(
    val timeMillis: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long,
)

data class ExnessAccessPoint(val accessPoint: String, val accountId: String)

/** Native, signed, read-only Exness Trader API client. This class deliberately has no order methods. */
class ExnessReadOnlyClient(
    private val credentials: ExnessCredentialVault.Credentials,
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .followRedirects(false)
        .build(),
) {
    fun discoverAccessPoint(): ExnessAccessPoint {
        val account = accountId()
        val path = "/v1/trading/access-point?account_id=$account"
        val json = signedGet(credentials.gateway, path)
        val access = json.optString("access_point")
        require(access.startsWith("https://")) { "Exness returned an invalid access point." }
        return ExnessAccessPoint(access.trimEnd('/'), json.optString("account_id", account))
    }

    fun loadCandles(
        accessPoint: String,
        timeframe: Timeframe,
        count: Int = 500,
        from: Instant = Instant.now().minusSeconds(timeframe.durationMillis / 1000L * count),
    ): List<ExnessCandle> {
        require(count in 1..1000) { "Candle count must be between 1 and 1000; use configured API limits for larger history." }
        val path = ExnessMarketData.candlesPath(
            accountId = accountId(),
            instrument = credentials.instrument,
            timeframe = timeframe,
            fromIso8601 = from.toString(),
            count = count,
        )
        val json = signedGet(accessPoint, path)
        val rows = json.optJSONArray("candles") ?: return emptyList()
        return buildList(rows.length()) {
            for (i in 0 until rows.length()) {
                val row = rows.getJSONObject(i)
                val time = Instant.parse(row.getString("time")).toEpochMilli()
                val bid = row.optJSONObject("bid")
                val ask = row.optJSONObject("ask")
                val open = bid?.optDouble("open") ?: row.optDouble("open", Double.NaN)
                val high = bid?.optDouble("high") ?: row.optDouble("high", Double.NaN)
                val low = bid?.optDouble("low") ?: row.optDouble("low", Double.NaN)
                val close = bid?.optDouble("close") ?: row.optDouble("close", Double.NaN)
                val askClose = ask?.optDouble("close") ?: close
                val volume = row.optDouble("volume", 0.0).toLong()
                if (open.isFinite() && high >= maxOf(open, close) && low <= minOf(open, close) &&
                    close.isFinite() && askClose.isFinite() && volume >= 0) {
                    add(ExnessCandle(time, open, high, low, close, volume, askClose))
                }
            }
        }.sortedBy { it.timeMillis }
    }

    private fun signedGet(base: String, pathWithQuery: String): JSONObject {
        require(base.startsWith("https://")) { "Only HTTPS Exness gateways are allowed." }
        require(pathWithQuery.startsWith("/") && !pathWithQuery.startsWith("//"))
        val apiKey = credentials.apiKey.trim()
        require(apiKey.isNotEmpty())
        val timestamp = System.currentTimeMillis()
        val bodyHash = base64Url(MessageDigest.getInstance("SHA-256").digest(ByteArray(0)))
        val payload = JSONObject()
            .put("api_key", apiKey)
            .put("idempotency_key", "")
            .put("timestamp", timestamp)
            .put("sign_version", 1)
            .put("method", "GET")
            .put("path", pathWithQuery)
            .put("body_hash", bodyHash)
            .toString()
            .toByteArray(StandardCharsets.UTF_8)
        val signature = sign(payload, credentials.privateKey)
        val data = base64Url(payload)
        val request = Request.Builder()
            .url(base.trimEnd('/') + pathWithQuery)
            .get()
            .header("Accept", "application/json")
            .header("EXN-API-KEY", apiKey)
            .header("EXN-IDEMPOTENCY-KEY", "")
            .header("EXN-TIMESTAMP", timestamp.toString())
            .header("EXN-SIGN-VERSION", "1")
            .header("EXN-DATA", data)
            .header("EXN-SIGN", signature)
            .build()
        http.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val publicMessage = runCatching { JSONObject(responseBody).optString("error_message") }.getOrNull()
                throw IllegalStateException(publicMessage?.takeIf { it.isNotBlank() } ?: "Exness API request failed (HTTP ${response.code}).")
            }
            return JSONObject(responseBody)
        }
    }

    private fun accountId(): String {
        val id = credentials.accountId.trim()
        require(Regex("^[0-9]{1,20}$").matches(id)) { "Account ID must contain 1 to 20 digits." }
        return id
    }

    private fun sign(payload: ByteArray, privateKey: String): String {
        val keyText = privateKey.trim()
        val hex = keyText.replace(Regex("\\s+"), "")
        val seed = if (keyText.contains("BEGIN")) {
            val body = keyText.lines().filterNot { it.startsWith("-----") }.joinToString("")
            val der = Base64.decode(body, Base64.DEFAULT)
            require(der.size >= 32) { "Invalid PKCS#8 Ed25519 private key." }
            der.copyOfRange(der.size - 32, der.size)
        } else {
            require(Regex("^(?:[0-9a-fA-F]{64}|[0-9a-fA-F]{128})$").matches(hex)) {
                "Private key must be a 32-byte seed, 64-byte secret, or PKCS#8 PEM."
            }
            hex.take(64).chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        }
        val signer = Ed25519Signer()
        signer.init(true, Ed25519PrivateKeyParameters(seed, 0))
        signer.update(payload, 0, payload.size)
        return base64Url(signer.generateSignature())
    }

    private fun base64Url(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)

    companion object {
        fun encodeQuery(value: String): String =
            URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")
    }
}

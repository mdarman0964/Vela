package com.mdarman0964.vela

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Background = Color(0xFF080D13)
private val Panel = Color(0xFF101A24)
private val Neon = Color(0xFF4DE6C1)
private val Muted = Color(0xFF91A4B7)
private val Red = Color(0xFFFF6B78)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = TimeframePreferences(applicationContext)
        val vault = ExnessCredentialVault(applicationContext)
        val restored = vault.load()
        setContent {
            MaterialTheme {
                val selected by preferences.timeframe.collectAsState(initial = Timeframe.default)
                val scope = rememberCoroutineScope()
                var gateway by remember { mutableStateOf(restored?.gateway.orEmpty()) }
                var accountId by remember { mutableStateOf(restored?.accountId.orEmpty()) }
                var apiKey by remember { mutableStateOf(restored?.apiKey.orEmpty()) }
                var privateKey by remember { mutableStateOf(restored?.privateKey.orEmpty()) }
                var activeCredentials by remember { mutableStateOf<ExnessCredentialVault.Credentials?>(null) }
                var accessPoint by remember { mutableStateOf<String?>(null) }
                var candles by remember { mutableStateOf<List<ExnessCandle>>(emptyList()) }
                var latestTick by remember { mutableStateOf<ExnessTick?>(null) }
                var tickStatus by remember { mutableStateOf("Waiting for live tick stream") }
                var loadedTimeframe by remember { mutableStateOf<Timeframe?>(null) }
                var status by remember { mutableStateOf(if (restored == null) "Credentials not configured" else "Credentials encrypted on this device · Connect to load market data") }
                var busy by remember { mutableStateOf(false) }
                var pointSize by remember { mutableStateOf<Double?>(null) }
                var candleLimit by remember { mutableStateOf<Int?>(null) }
                var strategySignal by remember { mutableStateOf<StrategySignal?>(null) }
                var lastSignalKey by remember { mutableStateOf<String?>(null) }

                DisposableEffect(activeCredentials, accessPoint) {
                    val credentials = activeCredentials
                    val endpoint = accessPoint
                    val socket = if (credentials != null && endpoint != null) {
                        ExnessReadOnlyClient(credentials).openTickStream(
                            endpoint,
                            onTick = { tick ->
                                runOnUiThread {
                                    latestTick = tick
                                    tickStatus = "Live tick received"
                                }
                            },
                            onError = { error ->
                                runOnUiThread { tickStatus = error }
                            },
                        )
                    } else null
                    onDispose {
                        socket?.close(1000, "VELA screen closed")
                    }
                }

                LaunchedEffect(candles, selected, pointSize, latestTick) {
                    val point = pointSize
                    val latest = candles.lastOrNull()
                    val tick = latestTick
                    val tickBarOpen = tick?.timeMillis?.let { it - (it % selected.durationMillis) }
                    if (loadedTimeframe == selected && candles.size >= 201 && point != null &&
                        latest != null && tick != null && latest.timeMillis == tickBarOpen) {
                        val key = "${selected.apiValue}-${latest.timeMillis}"
                        if (key != lastSignalKey) {
                            val series = candles.map { candle ->
                                MarketCandle(candle.timeMillis, candle.open, candle.high, candle.low, candle.close, candle.volume, candle.close, candle.askClose)
                            }.toMutableList()
                            val last = series.last()
                            series[series.lastIndex] = last.copy(
                                high = maxOf(last.high, tick.bid),
                                low = minOf(last.low, tick.bid),
                                close = tick.bid,
                                bid = tick.bid,
                                ask = tick.ask,
                            )
                            strategySignal = withContext(Dispatchers.Default) {
                                Mql5StrategyEngine.evaluate(selected, series, point)
                            }
                            lastSignalKey = key
                        }
                    } else {
                        strategySignal = null
                        lastSignalKey = null
                    }
                }

                LaunchedEffect(selected, activeCredentials, accessPoint, candleLimit) {
                    val credentials = activeCredentials
                    val endpoint = accessPoint
                    if (credentials != null && endpoint != null && candleLimit != null && candleLimit!! > 0) {
                        while (isActive) {
                            try {
                                val loaded = withContext(Dispatchers.IO) {
                                    ExnessReadOnlyClient(credentials).loadCandles(endpoint, selected, count = minOf(300, candleLimit!!))
                                }
                                candles = loaded
                                loadedTimeframe = selected
                                status = if (loaded.isEmpty()) "Connected · no candles returned for this interval" else "Connected · ${loaded.size} candles · refreshed"
                            } catch (error: Exception) {
                                status = error.message?.take(140) ?: "Exness candle request failed"
                            }
                            delay(15_000)
                        }
                    }
                }

                Surface(modifier = Modifier.fillMaxSize(), color = Background) {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("VELA", color = Neon, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                                Text("Volume Profile Bounce", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Text("NATIVE", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
                        }
                        Column(
                            modifier = Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(18.dp))
                                .border(1.dp, Color(0xFF20313F), RoundedCornerShape(18.dp)).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("INSTRUMENT", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
                            Text("XAUUSDm", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                            Text("TIMEFRAME", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                                Timeframe.entries.forEach { item ->
                                    val active = selected == item
                                    TextButton(
                                        onClick = { scope.launch { preferences.setTimeframe(item) } },
                                        modifier = Modifier.weight(1f)
                                            .background(if (active) Neon.copy(alpha = 0.16f) else Color(0xFF0B121A), RoundedCornerShape(10.dp))
                                            .border(1.dp, if (active) Neon else Color(0xFF263746), RoundedCornerShape(10.dp))
                                    ) {
                                        Text(item.label, color = if (active) Neon else Muted, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Text("Selected API interval: ${selected.apiValue}", color = Neon, fontSize = 12.sp)
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(18.dp))
                                .border(1.dp, Color(0xFF20313F), RoundedCornerShape(18.dp)).padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            Text("EXNESS CONNECTION", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
                            OutlinedTextField(gateway, { gateway = it }, label = { Text("API Gateway URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(accountId, { accountId = it }, label = { Text("Account ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(apiKey, { apiKey = it }, label = { Text("API Key ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(privateKey, { privateKey = it }, label = { Text("Ed25519 private key / seed") }, singleLine = true,
                                visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    enabled = !busy,
                                    onClick = {
                                        scope.launch {
                                            busy = true
                                            status = "Saving credentials securely and discovering Exness access point…"
                                            try {
                                                val creds = ExnessCredentialVault.Credentials(gateway.trim(), accountId.trim(), apiKey.trim(), privateKey.trim())
                                                withContext(Dispatchers.IO) {
                                                    vault.save(creds)
                                                    val client = ExnessReadOnlyClient(creds)
                                                    val found = client.discoverAccessPoint()
                                                    val point = client.loadPointSize(found.accessPoint)
                                                    val maxCandles = client.loadCandleRequestLimit(found.accessPoint)
                                                    Triple(found, point, maxCandles)
                                                }.also { (found, point, maxCandles) ->
                                                    accessPoint = found.accessPoint
                                                    pointSize = point
                                                    candleLimit = maxCandles
                                                    lastSignalKey = null
                                                    activeCredentials = creds
                                                    status = "Access point verified · point $point · candle limit $maxCandles · loading ${selected.apiValue}"
                                                }
                                            } catch (error: Exception) {
                                                status = error.message?.take(140) ?: "Connection failed"
                                            } finally {
                                                busy = false
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Neon, contentColor = Background)
                                ) { Text(if (busy) "CONNECTING…" else "CONNECT & LOAD") }
                                TextButton(onClick = {
                                    vault.clear()
                                    activeCredentials = null
                                    accessPoint = null
                                    pointSize = null
                                    candleLimit = null
                                    candles = emptyList()
                                    loadedTimeframe = null
                                    latestTick = null
                                    strategySignal = null
                                    lastSignalKey = null
                                    gateway = ""; accountId = ""; apiKey = ""; privateKey = ""
                                    tickStatus = "Tick stream stopped"
                                    status = "Saved credentials cleared"
                                }) { Text("CLEAR", color = Red) }
                            }
                            Text(status, color = if (status.startsWith("Connected")) Neon else Muted, fontSize = 11.sp)
                            Text("Keys are AES-GCM encrypted with an Android Keystore key. Requests are signed on-device; no VELA server.", color = Muted, fontSize = 10.sp)
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(18.dp))
                                .border(1.dp, Color(0xFF20313F), RoundedCornerShape(18.dp)).padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("XAUUSDm · ${selected.label}", color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Text(if (candles.isNotEmpty()) "EXNESS DATA" else "NO FEED", color = if (candles.isNotEmpty()) Neon else Muted, fontSize = 9.sp)
                            }
                            if (candles.isNotEmpty()) {
                                val tick = latestTick
                                if (tick != null) {
                                    Text("LIVE  BID ${"%.2f".format(tick.bid)}  /  ASK ${"%.2f".format(tick.ask)}", color = Neon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Text("Waiting for live bid/ask ticks · ${tickStatus.take(80)}", color = Muted, fontSize = 11.sp)
                                }
                                Text("History ${candles.size} candles · refreshes every 15 seconds", color = Muted, fontSize = 10.sp)
                                CandleChart(candles, tick, Modifier.fillMaxWidth().height(230.dp))
                            } else {
                                Text("No market prices are fabricated. Connect your Exness API credentials to load real bid/ask candles.", color = Muted, fontSize = 13.sp)
                                Spacer(Modifier.height(120.dp))
                            }
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(18.dp)).padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("STRATEGY ENGINE · OBSERVE ONLY", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
                            Text("Volume Profile · SMA 50 · ATR 14", color = Color.White, fontSize = 14.sp)
                            Text("Live order execution: OFF", color = Neon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            val signal = strategySignal
                            if (signal != null) {
                                Text("Signal: ${if (signal.buy) "BUY" else if (signal.sell) "SELL" else "WAIT"}", color = if (signal.buy || signal.sell) Neon else Muted, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                signal.profile?.let { profile ->
                                    Text("POC ${"%.2f".format(profile.poc)} · VAH ${"%.2f".format(profile.vah)} · VAL ${"%.2f".format(profile.valPrice)}", color = Color.White, fontSize = 11.sp)
                                }
                                Text("SMA ${signal.sma?.let { "%.2f".format(it) } ?: "—"} · ATR ${signal.atr?.let { "%.2f".format(it) } ?: "—"}", color = Muted, fontSize = 11.sp)
                                Text(signal.reason, color = Muted, fontSize = 10.sp)
                            } else {
                                Text("Strategy waiting for at least 201 real candles and Exness instrument precision.", color = Muted, fontSize = 10.sp)
                            }
                            Text("Entry checks run once per new selected-timeframe candle, like MQL5 OnTick bar gating. No broker orders are sent.", color = Muted, fontSize = 10.sp)
                        }
                        Text("Native Android · HTTPS only · No external backend · Candle refresh 15s", color = Muted, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun CandleChart(candles: List<ExnessCandle>, latestTick: ExnessTick?, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFF0A1119))) {
        if (candles.isEmpty()) return@Canvas
        val visible = candles.takeLast(100)
        val minPrice = visible.minOf { it.low }
        val maxPrice = visible.maxOf { it.high }
        val range = (maxPrice - minPrice).takeIf { it > 0.0 } ?: 1.0
        val cell = size.width / visible.size
        val bodyWidth = (cell * 0.58f).coerceAtLeast(1.5f)
        fun y(price: Double) = size.height - (((price - minPrice) / range).toFloat() * size.height)
        for (line in 1..3) {
            val yy = size.height * line / 4f
            drawLine(Color(0xFF1C2A36), androidx.compose.ui.geometry.Offset(0f, yy),
                androidx.compose.ui.geometry.Offset(size.width, yy), strokeWidth = 1f)
        }
        if (latestTick != null && latestTick.bid in minPrice..maxPrice) {
            val liveY = y(latestTick.bid)
            drawLine(Color(0xFFFFD166), androidx.compose.ui.geometry.Offset(0f, liveY),
                androidx.compose.ui.geometry.Offset(size.width, liveY), strokeWidth = 1.5f)
        }
        visible.forEachIndexed { index, candle ->
            val x = (index + 0.5f) * cell
            val rising = candle.close >= candle.open
            val tint = if (rising) Neon else Red
            drawLine(tint, androidx.compose.ui.geometry.Offset(x, y(candle.high)),
                androidx.compose.ui.geometry.Offset(x, y(candle.low)), strokeWidth = 1.2f)
            val top = minOf(y(candle.open), y(candle.close))
            val bottom = maxOf(y(candle.open), y(candle.close))
            drawRect(tint, androidx.compose.ui.geometry.Offset(x - bodyWidth / 2f, top),
                androidx.compose.ui.geometry.Size(bodyWidth, (bottom - top).coerceAtLeast(2f)))
        }
    }
}

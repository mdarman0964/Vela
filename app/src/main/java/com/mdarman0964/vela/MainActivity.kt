package com.mdarman0964.vela

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Background = Color(0xFF080D13)
private val Panel = Color(0xFF101A24)
private val Neon = Color(0xFF4DE6C1)
private val Muted = Color(0xFF91A4B7)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var timeframeName by rememberSaveable { mutableStateOf(Timeframe.default.name) }
                val selected = Timeframe.valueOf(timeframeName)
                Surface(modifier = Modifier.fillMaxSize(), color = Background) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("VELA", color = Neon, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                                Text("Volume Profile Bounce", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Text("NATIVE", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
                        }
                        Column(
                            modifier = Modifier.fillMaxWidth()
                                .background(Panel, RoundedCornerShape(18.dp))
                                .border(1.dp, Color(0xFF20313F), RoundedCornerShape(18.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("INSTRUMENT", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
                            Text("XAUUSDm", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                            Text("TIMEFRAME", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Timeframe.entries.forEach { item ->
                                    val active = selected == item
                                    TextButton(
                                        onClick = { timeframeName = item.name },
                                        modifier = Modifier.weight(1f)
                                            .background(if (active) Neon.copy(alpha = 0.16f) else Color(0xFF0B121A), RoundedCornerShape(10.dp))
                                            .border(1.dp, if (active) Neon else Color(0xFF263746), RoundedCornerShape(10.dp))
                                    ) {
                                        Text(item.label, color = if (active) Neon else Muted, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Text("Selected candle interval: ${selected.apiValue}", color = Neon, fontSize = 12.sp)
                        }
                        Column(
                            modifier = Modifier.fillMaxWidth()
                                .weight(1f)
                                .background(Panel, RoundedCornerShape(18.dp))
                                .border(1.dp, Color(0xFF20313F), RoundedCornerShape(18.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("MARKET CHART", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Waiting for Exness connection", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                            Text("The chart will load ${selected.label} candles after a broker connection is configured.", color = Muted, fontSize = 13.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Text("STRATEGY", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
                            Text("Volume Profile · SMA 50 · ATR 14", color = Color.White, fontSize = 14.sp)
                            Text("Live order execution: OFF", color = Neon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("No external server · Native Android · Exness data requires internet", color = Muted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

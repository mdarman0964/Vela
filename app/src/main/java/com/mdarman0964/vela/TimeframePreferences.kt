package com.mdarman0964.vela

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.velaDataStore by preferencesDataStore(name = "vela_preferences")

class TimeframePreferences(private val context: Context) {
    private val timeframeKey = stringPreferencesKey("selected_timeframe")

    val timeframe: Flow<Timeframe> = context.velaDataStore.data.map { preferences ->
        Timeframe.fromApiValue(preferences[timeframeKey] ?: Timeframe.default.apiValue)
    }

    suspend fun setTimeframe(value: Timeframe) {
        context.velaDataStore.edit { preferences ->
            preferences[timeframeKey] = value.apiValue
        }
    }
}

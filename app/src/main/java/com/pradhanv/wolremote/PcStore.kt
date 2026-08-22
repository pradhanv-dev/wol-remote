package com.pradhanv.wolremote

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "pcs")

class PcStore(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val key = stringPreferencesKey("pcs_json")

    val pcs: Flow<List<PcEntry>> = context.dataStore.data.map { prefs ->
        prefs[key]?.let { runCatching { json.decodeFromString<List<PcEntry>>(it) }.getOrDefault(emptyList()) } ?: emptyList()
    }

    suspend fun save(list: List<PcEntry>) {
        context.dataStore.edit { it[key] = json.encodeToString(list) }
    }

    suspend fun upsert(entry: PcEntry): List<PcEntry> {
        val cur = pcs.first()
        val next = if (cur.any { it.id == entry.id }) cur.map { if (it.id == entry.id) entry else it }
                   else cur + entry.copy(id = (cur.maxOfOrNull { it.id } ?: 0L) + 1L)
        save(next); return next
    }

    suspend fun remove(id: Long): List<PcEntry> {
        val next = pcs.first().filterNot { it.id == id }
        save(next); return next
    }
}

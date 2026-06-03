package eu.slickbot.avimar.data.search

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.searchDataStore by preferencesDataStore(name = "avimar_search")

class SearchHistoryRepository(
  private val context: Context,
  private val json: Json,
) {

  companion object {
    private val KEY_RECENTS = stringPreferencesKey("recent_entities")
    private const val MAX_RECENTS = 8
  }

  private val store get() = context.applicationContext.searchDataStore

  val recentEntities: Flow<List<RecentEntity>> = store.data.map { prefs ->
    decode(prefs[KEY_RECENTS])
  }

  suspend fun add(entity: RecentEntity) {
    store.edit { prefs ->
      val next = (listOf(entity) + withoutKey(decode(prefs[KEY_RECENTS]), entity)).take(MAX_RECENTS)
      prefs[KEY_RECENTS] = json.encodeToString(next)
    }
  }

  suspend fun remove(entity: RecentEntity) {
    store.edit { prefs ->
      prefs[KEY_RECENTS] = json.encodeToString(withoutKey(decode(prefs[KEY_RECENTS]), entity))
    }
  }

  suspend fun clear() {
    store.edit { it.remove(KEY_RECENTS) }
  }

  private fun withoutKey(list: List<RecentEntity>, entity: RecentEntity): List<RecentEntity> {
    return list.filterNot { it.kind == entity.kind && it.id.equals(entity.id, ignoreCase = true) }
  }

  private fun decode(raw: String?): List<RecentEntity> {
    if (raw == null) return emptyList()
    return runCatching { json.decodeFromString<List<RecentEntity>>(raw) }.getOrDefault(emptyList())
  }
}

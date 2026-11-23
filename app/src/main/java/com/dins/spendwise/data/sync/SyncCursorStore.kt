package com.dins.spendwise.data.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class SyncCursorStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    suspend fun getCursor(): String? = dataStore.data.first()[KEY_CURSOR]

    suspend fun setCursor(cursor: String?) {
        dataStore.edit { prefs ->
            if (cursor.isNullOrBlank()) {
                prefs.remove(KEY_CURSOR)
            } else {
                prefs[KEY_CURSOR] = cursor
            }
        }
    }

    companion object {
        private val KEY_CURSOR = stringPreferencesKey("sync_pull_cursor")
    }
}

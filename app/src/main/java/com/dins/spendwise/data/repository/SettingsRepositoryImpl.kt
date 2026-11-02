package com.dins.spendwise.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dins.spendwise.domain.model.ThemePreference
import com.dins.spendwise.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    override fun observeThemePreference(): Flow<ThemePreference> =
        dataStore.data.map { prefs ->
            prefs[THEME_KEY]?.let {
                runCatching { ThemePreference.valueOf(it) }.getOrDefault(ThemePreference.SYSTEM)
            } ?: ThemePreference.SYSTEM
        }

    override suspend fun setThemePreference(preference: ThemePreference) {
        dataStore.edit { prefs ->
            prefs[THEME_KEY] = preference.name
        }
    }

    companion object {
        private val THEME_KEY = stringPreferencesKey("theme_preference")
    }
}

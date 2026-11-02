package com.dins.spendwise.domain.repository

import com.dins.spendwise.domain.model.ThemePreference
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeThemePreference(): Flow<ThemePreference>
    suspend fun setThemePreference(preference: ThemePreference)
}

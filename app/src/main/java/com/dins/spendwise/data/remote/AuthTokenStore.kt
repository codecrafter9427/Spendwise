package com.dins.spendwise.data.remote

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochMs: Long,
    val userId: String?,
    val email: String?
)

@Singleton
class AuthTokenStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val sessionFlow: Flow<AuthSession?> = dataStore.data.map { prefs ->
        val access = prefs[ACCESS_TOKEN] ?: return@map null
        val refresh = prefs[REFRESH_TOKEN] ?: return@map null
        AuthSession(
            accessToken = access,
            refreshToken = refresh,
            expiresAtEpochMs = prefs[EXPIRES_AT] ?: 0L,
            userId = prefs[USER_ID],
            email = prefs[EMAIL]
        )
    }

    suspend fun currentSession(): AuthSession? = sessionFlow.first()

    suspend fun save(session: AuthSession) {
        dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = session.accessToken
            prefs[REFRESH_TOKEN] = session.refreshToken
            prefs[EXPIRES_AT] = session.expiresAtEpochMs
            if (session.userId != null) prefs[USER_ID] = session.userId else prefs.remove(USER_ID)
            if (session.email != null) prefs[EMAIL] = session.email else prefs.remove(EMAIL)
        }
    }

    suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(ACCESS_TOKEN)
            prefs.remove(REFRESH_TOKEN)
            prefs.remove(EXPIRES_AT)
            prefs.remove(USER_ID)
            prefs.remove(EMAIL)
            prefs.remove(IMPORT_DONE)
        }
    }

    suspend fun isImportDone(): Boolean =
        dataStore.data.first()[IMPORT_DONE] == true

    suspend fun setImportDone(done: Boolean) {
        dataStore.edit { prefs ->
            prefs[IMPORT_DONE] = done
        }
    }

    companion object {
        private val ACCESS_TOKEN = stringPreferencesKey("auth_access_token")
        private val REFRESH_TOKEN = stringPreferencesKey("auth_refresh_token")
        private val EXPIRES_AT = longPreferencesKey("auth_expires_at")
        private val USER_ID = stringPreferencesKey("auth_user_id")
        private val EMAIL = stringPreferencesKey("auth_email")
        private val IMPORT_DONE = booleanPreferencesKey("auth_import_done")
    }
}

package com.dins.spendwise.data.remote

import com.dins.spendwise.BuildConfig
import com.dins.spendwise.data.remote.dto.SupabaseRefreshRequest
import com.dins.spendwise.data.remote.dto.SupabaseTokenRequest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: SupabaseAuthApi,
    private val tokenStore: AuthTokenStore
) {
    val isSignedIn: Flow<Boolean> = tokenStore.sessionFlow.map { it != null }
    val sessionFlow: Flow<AuthSession?> = tokenStore.sessionFlow

    fun isConfigured(): Boolean =
        BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    suspend fun signIn(email: String, password: String): Result<AuthSession> = runCatching {
        requireConfigured()
        val key = BuildConfig.SUPABASE_ANON_KEY
        val response = authApi.signIn(
            apiKey = key,
            authorization = "Bearer $key",
            body = SupabaseTokenRequest(email = email.trim(), password = password)
        )
        val session = response.toSession()
        tokenStore.save(session)
        session
    }.recoverCatching { error ->
        throw mapAuthError(error)
    }

    suspend fun signUp(email: String, password: String): Result<AuthSession> = runCatching {
        requireConfigured()
        val key = BuildConfig.SUPABASE_ANON_KEY
        val response = authApi.signUp(
            apiKey = key,
            authorization = "Bearer $key",
            body = SupabaseTokenRequest(email = email.trim(), password = password)
        )
        val session = response.toSession()
        tokenStore.save(session)
        session
    }.recoverCatching { error ->
        throw mapAuthError(error)
    }

    suspend fun signOut() {
        tokenStore.clear()
    }

    suspend fun validAccessToken(): String? {
        val session = tokenStore.currentSession() ?: return null
        val now = System.currentTimeMillis()
        if (session.expiresAtEpochMs > now + 30_000L) {
            return session.accessToken
        }
        return refresh(session.refreshToken).getOrNull()?.accessToken
    }

    suspend fun refresh(refreshToken: String): Result<AuthSession> = runCatching {
        requireConfigured()
        val key = BuildConfig.SUPABASE_ANON_KEY
        val response = authApi.refresh(
            apiKey = key,
            authorization = "Bearer $key",
            body = SupabaseRefreshRequest(refreshToken = refreshToken)
        )
        val session = response.toSession()
        tokenStore.save(session)
        session
    }

    private fun requireConfigured() {
        check(isConfigured()) {
            "Supabase is not configured. Add spendwise.supabase.url and spendwise.supabase.anonKey to local.properties"
        }
    }

    private fun mapAuthError(error: Throwable): Throwable {
        if (error is HttpException) {
            return IllegalStateException("Auth failed (${error.code()}). Check email/password or Supabase Email provider.")
        }
        return error
    }

    private fun com.dins.spendwise.data.remote.dto.SupabaseAuthResponse.toSession(): AuthSession {
        val expiresInMs = (expiresIn ?: 3600L) * 1000L
        return AuthSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAtEpochMs = System.currentTimeMillis() + expiresInMs,
            userId = user?.id,
            email = user?.email
        )
    }
}

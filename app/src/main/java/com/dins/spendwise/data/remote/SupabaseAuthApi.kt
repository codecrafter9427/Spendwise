package com.dins.spendwise.data.remote

import com.dins.spendwise.data.remote.dto.SupabaseAuthResponse
import com.dins.spendwise.data.remote.dto.SupabaseRefreshRequest
import com.dins.spendwise.data.remote.dto.SupabaseTokenRequest
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseAuthApi {
    @POST("/auth/v1/signup")
    suspend fun signUp(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body body: SupabaseTokenRequest
    ): SupabaseAuthResponse

    @POST("/auth/v1/token")
    suspend fun signIn(
        @Query("grant_type") grantType: String = "password",
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body body: SupabaseTokenRequest
    ): SupabaseAuthResponse

    @POST("/auth/v1/token")
    suspend fun refresh(
        @Query("grant_type") grantType: String = "refresh_token",
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body body: SupabaseRefreshRequest
    ): SupabaseAuthResponse
}

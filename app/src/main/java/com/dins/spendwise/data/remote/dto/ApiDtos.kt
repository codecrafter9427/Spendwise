package com.dins.spendwise.data.remote.dto

import com.squareup.moshi.Json

data class ApiErrorEnvelope(
    val error: ApiErrorBody?
)

data class ApiErrorBody(
    val code: String?,
    val message: String?
)

data class UserProfileDto(
    val id: String,
    @Json(name = "display_name") val displayName: String?,
    @Json(name = "base_currency") val baseCurrency: String,
    val timezone: String
)

data class CategoryDto(
    val id: String,
    @Json(name = "user_id") val userId: String?,
    val name: String,
    val type: String,
    @Json(name = "icon_key") val iconKey: String,
    @Json(name = "is_default") val isDefault: Boolean
)

data class TransactionCreateDto(
    val id: String,
    val type: String,
    @Json(name = "amount_minor") val amountMinor: Long,
    val currency: String = "INR",
    @Json(name = "category_id") val categoryId: String,
    val description: String?,
    @Json(name = "transaction_date") val transactionDate: String
)

data class TransactionUpdateDto(
    @Json(name = "expected_version") val expectedVersion: Int,
    val type: String? = null,
    @Json(name = "amount_minor") val amountMinor: Long? = null,
    val currency: String? = null,
    @Json(name = "category_id") val categoryId: String? = null,
    val description: String? = null,
    @Json(name = "transaction_date") val transactionDate: String? = null
)

data class TransactionDto(
    val id: String,
    val type: String,
    @Json(name = "amount_minor") val amountMinor: Long,
    val currency: String,
    @Json(name = "category_id") val categoryId: String,
    val description: String?,
    @Json(name = "transaction_date") val transactionDate: String,
    val version: Int,
    @Json(name = "deleted_at") val deletedAt: String?
)

data class TransactionPageDto(
    val items: List<TransactionDto>,
    @Json(name = "next_cursor") val nextCursor: String?,
    val limit: Int
)

data class BudgetUpsertDto(
    @Json(name = "amount_minor") val amountMinor: Long,
    val currency: String = "INR",
    @Json(name = "expected_version") val expectedVersion: Int? = null
)

data class BudgetDto(
    val id: String,
    val year: Int,
    val month: Int,
    @Json(name = "amount_minor") val amountMinor: Long,
    val currency: String,
    val version: Int
)

data class SupabaseTokenRequest(
    val email: String,
    val password: String
)

data class SupabaseRefreshRequest(
    @Json(name = "refresh_token") val refreshToken: String
)

data class SupabaseAuthResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "refresh_token") val refreshToken: String,
    @Json(name = "expires_in") val expiresIn: Long?,
    val user: SupabaseUser?
)

data class SupabaseUser(
    val id: String,
    val email: String?
)

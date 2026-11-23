package com.dins.spendwise.data.remote.dto

import com.squareup.moshi.Json

data class SyncPushRequestDto(
    val operations: List<SyncPushOperationDto>
)

data class SyncPushOperationDto(
    @Json(name = "operation_id") val operationId: String,
    @Json(name = "entity_type") val entityType: String,
    @Json(name = "entity_id") val entityId: String,
    val operation: String,
    @Json(name = "base_version") val baseVersion: Int? = null,
    val payload: Map<String, Any?>? = null
)

data class SyncPushResponseDto(
    val results: List<SyncPushItemResultDto>
)

data class SyncPushItemResultDto(
    @Json(name = "operation_id") val operationId: String,
    val status: String,
    @Json(name = "entity_id") val entityId: String? = null,
    val version: Int? = null,
    @Json(name = "error_code") val errorCode: String? = null,
    @Json(name = "error_message") val errorMessage: String? = null
)

data class SyncPullResponseDto(
    val changes: List<SyncChangeDto>,
    @Json(name = "next_cursor") val nextCursor: String?,
    @Json(name = "has_more") val hasMore: Boolean
)

data class SyncChangeDto(
    val sequence: Long,
    @Json(name = "entity_type") val entityType: String,
    @Json(name = "entity_id") val entityId: String,
    val operation: String,
    val version: Int,
    val payload: Map<String, Any?>?,
    @Json(name = "changed_at") val changedAt: String
)

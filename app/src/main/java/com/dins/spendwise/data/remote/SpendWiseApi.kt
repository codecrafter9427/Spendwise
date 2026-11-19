package com.dins.spendwise.data.remote

import com.dins.spendwise.data.remote.dto.BudgetDto
import com.dins.spendwise.data.remote.dto.BudgetUpsertDto
import com.dins.spendwise.data.remote.dto.CategoryDto
import com.dins.spendwise.data.remote.dto.TransactionCreateDto
import com.dins.spendwise.data.remote.dto.TransactionDto
import com.dins.spendwise.data.remote.dto.TransactionUpdateDto
import com.dins.spendwise.data.remote.dto.UserProfileDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface SpendWiseApi {
    @GET("/api/v1/me")
    suspend fun getMe(): UserProfileDto

    @GET("/api/v1/categories")
    suspend fun listCategories(): List<CategoryDto>

    @POST("/api/v1/transactions")
    suspend fun createTransaction(@Body body: TransactionCreateDto): TransactionDto

    @PATCH("/api/v1/transactions/{id}")
    suspend fun updateTransaction(
        @Path("id") id: String,
        @Body body: TransactionUpdateDto
    ): TransactionDto

    @DELETE("/api/v1/transactions/{id}")
    suspend fun deleteTransaction(
        @Path("id") id: String,
        @Query("expected_version") expectedVersion: Int
    ): TransactionDto

    @PUT("/api/v1/budgets/{year}/{month}")
    suspend fun upsertBudget(
        @Path("year") year: Int,
        @Path("month") month: Int,
        @Body body: BudgetUpsertDto
    ): BudgetDto
}

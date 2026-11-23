package com.dins.spendwise.di

import com.dins.spendwise.BuildConfig
import com.dins.spendwise.data.remote.AuthInterceptor
import com.dins.spendwise.data.remote.MoshiObjectAdapter
import com.dins.spendwise.data.remote.SpendWiseApi
import com.dins.spendwise.data.remote.SupabaseAuthApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(MoshiObjectAdapter())
        .add(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    @Named("spendwise")
    fun provideSpendWiseRetrofit(client: OkHttpClient, moshi: Moshi): Retrofit {
        val base = BuildConfig.API_BASE_URL.trimEnd('/') + "/"
        return Retrofit.Builder()
            .baseUrl(base)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    @Named("supabase")
    fun provideSupabaseRetrofit(moshi: Moshi): Retrofit {
        val base = BuildConfig.SUPABASE_URL.ifBlank { "https://invalid.local" }.trimEnd('/') + "/"
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
        return Retrofit.Builder()
            .baseUrl(base)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideSpendWiseApi(@Named("spendwise") retrofit: Retrofit): SpendWiseApi =
        retrofit.create(SpendWiseApi::class.java)

    @Provides
    @Singleton
    fun provideSupabaseAuthApi(@Named("supabase") retrofit: Retrofit): SupabaseAuthApi =
        retrofit.create(SupabaseAuthApi::class.java)
}

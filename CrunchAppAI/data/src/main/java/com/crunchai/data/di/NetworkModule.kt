package com.crunchai.data.di

import com.crunchai.data.BuildConfig
import com.crunchai.data.remote.api.CrunchApi
import com.crunchai.data.remote.interceptor.AuthInterceptor
import com.crunchai.data.remote.interceptor.ConnectivityInterceptor
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideJson(): Json =
        Json {
            ignoreUnknownKeys = true // новые поля сервера не должны ломать приложение
            explicitNulls = false
        }

    @Provides
    @Singleton
    fun provideOkHttp(
        connectivity: ConnectivityInterceptor,
        auth: AuthInterceptor,
    ): OkHttpClient {
        val logging =
            HttpLoggingInterceptor().apply {
                // В релизе тела запросов (там токены и пароли) в лог не попадают
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
                redactHeader("Authorization")
            }
        return OkHttpClient.Builder()
            .addInterceptor(connectivity)
            .addInterceptor(auth)
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS) // анализ фото через Gemini бывает долгим
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        client: OkHttpClient,
        json: Json,
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideCrunchApi(retrofit: Retrofit): CrunchApi = retrofit.create(CrunchApi::class.java)
}
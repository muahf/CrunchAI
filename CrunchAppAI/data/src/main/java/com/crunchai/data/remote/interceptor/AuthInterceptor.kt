package com.crunchai.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

interface TokenProvider {
    fun getToken(): String?
}

class AuthInterceptor
    @Inject
    constructor(
        private val tokenProvider: TokenProvider,
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val original = chain.request()
            val token = tokenProvider.getToken() ?: return chain.proceed(original)

            val request =
                original.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            return chain.proceed(request)
        }
    }

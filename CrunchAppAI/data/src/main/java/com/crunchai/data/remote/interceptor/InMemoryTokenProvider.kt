package com.crunchai.data.remote.interceptor

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryTokenProvider
    @Inject
    constructor() : TokenProvider {
        @Volatile
        private var token: String? = null

        override fun getToken(): String? = token

        fun updateToken(newToken: String?) {
            token = newToken
        }
    }

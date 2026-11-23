package com.crunchai.data.remote.interceptor

import com.crunchai.data.remote.network.NetworkMonitor
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject

class NoConnectivityException : IOException("Отсутствует подключение к интернету")

class ConnectivityInterceptor
    @Inject
    constructor(
        private val networkMonitor: NetworkMonitor,
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            if (!networkMonitor.isConnected()) {
                throw NoConnectivityException()
            }
            return chain.proceed(chain.request())
        }
    }

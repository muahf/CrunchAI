package com.crunchai.data.di

import com.crunchai.data.remote.interceptor.InMemoryTokenProvider
import com.crunchai.data.remote.interceptor.TokenProvider
import com.crunchai.data.remote.network.AndroidNetworkMonitor
import com.crunchai.data.remote.network.NetworkMonitor
import com.crunchai.data.repository.RemoteEventSyncRepositoryImpl
import com.crunchai.data.repository.RemoteFoodAnalysisRepositoryImpl
import com.crunchai.data.repository.RemotePairingRepositoryImpl
import com.example.crunchai.domain.repository.EventSyncRepository
import com.example.crunchai.domain.repository.FoodAnalysisRepository
import com.example.crunchai.domain.repository.PairingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindsModule {
    @Binds
    @Singleton
    abstract fun bindNetworkMonitor(impl: AndroidNetworkMonitor): NetworkMonitor

    @Binds
    @Singleton
    abstract fun bindTokenProvider(impl: InMemoryTokenProvider): TokenProvider

    @Binds
    @Singleton
    abstract fun bindEventSyncRepository(impl: RemoteEventSyncRepositoryImpl): EventSyncRepository

    @Binds
    @Singleton
    abstract fun bindPairingRepository(impl: RemotePairingRepositoryImpl): PairingRepository

    @Binds
    @Singleton
    abstract fun bindFoodAnalysisRepository(impl: RemoteFoodAnalysisRepositoryImpl): FoodAnalysisRepository
}

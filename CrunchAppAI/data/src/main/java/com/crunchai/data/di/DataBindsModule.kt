package com.crunchai.data.di

import com.crunchai.data.local.datastore.SessionPreferencesDataSource
import com.crunchai.data.remote.interceptor.TokenProvider
import com.crunchai.data.remote.network.AndroidNetworkMonitor
import com.crunchai.data.remote.network.NetworkMonitor
import com.crunchai.data.repository.LocalPetRepositoryImpl
import com.crunchai.data.repository.LocalSoundEventRepositoryImpl
import com.crunchai.data.repository.OfflineFirstDiaryRepositoryImpl
import com.crunchai.data.repository.RemoteEventSyncRepositoryImpl
import com.crunchai.data.repository.RemoteFoodAnalysisRepositoryImpl
import com.crunchai.data.repository.RemotePairingRepositoryImpl
import com.example.crunchai.domain.repository.DiaryRepository
import com.example.crunchai.domain.repository.EventSyncRepository
import com.example.crunchai.domain.repository.FoodAnalysisRepository
import com.example.crunchai.domain.repository.PairingRepository
import com.example.crunchai.domain.repository.PetRepository
import com.example.crunchai.domain.repository.SoundEventRepository
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
    abstract fun bindTokenProvider(impl: SessionPreferencesDataSource): TokenProvider

    @Binds
    @Singleton
    abstract fun bindEventSyncRepository(impl: RemoteEventSyncRepositoryImpl): EventSyncRepository

    @Binds
    @Singleton
    abstract fun bindPairingRepository(impl: RemotePairingRepositoryImpl): PairingRepository

    @Binds
    @Singleton
    abstract fun bindFoodAnalysisRepository(impl: RemoteFoodAnalysisRepositoryImpl): FoodAnalysisRepository

    @Binds
    @Singleton
    abstract fun bindSoundEventRepository(impl: LocalSoundEventRepositoryImpl): SoundEventRepository

    @Binds
    @Singleton
    abstract fun bindPetRepository(impl: LocalPetRepositoryImpl): PetRepository

    @Binds
    @Singleton
    abstract fun bindDiaryRepository(impl: OfflineFirstDiaryRepositoryImpl): DiaryRepository
}

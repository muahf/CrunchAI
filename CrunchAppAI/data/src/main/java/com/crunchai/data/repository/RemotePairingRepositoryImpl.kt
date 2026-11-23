package com.crunchai.data.repository

import com.crunchai.data.mapper.toDomain
import com.crunchai.data.remote.api.CrunchApi
import com.crunchai.data.remote.dto.PairRequestDto
import com.crunchai.data.remote.network.safeApiCall
import com.example.crunchai.domain.common.DomainResult
import com.example.crunchai.domain.model.PairingSession
import com.example.crunchai.domain.repository.PairingRepository
import javax.inject.Inject

class RemotePairingRepositoryImpl
    @Inject
    constructor(
        private val api: CrunchApi,
    ) : PairingRepository {
        override suspend fun createKey(): DomainResult<PairingSession> =
            safeApiCall {
                api.createPairKey().toDomain()
            }

        override suspend fun pair(key: String): DomainResult<PairingSession> =
            safeApiCall {
                api.pairWithStation(PairRequestDto(key = key)).toDomain()
            }
    }

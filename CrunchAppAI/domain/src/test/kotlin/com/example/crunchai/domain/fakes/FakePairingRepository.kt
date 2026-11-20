package com.example.crunchai.domain.fakes

import com.example.crunchai.domain.common.DomainResult
import com.example.crunchai.domain.model.DeviceRole
import com.example.crunchai.domain.model.PairingSession
import com.example.crunchai.domain.model.PairingStatus
import com.example.crunchai.domain.repository.PairingRepository

class FakePairingRepository(
    var createResult: DomainResult<PairingSession> =
        DomainResult.Success(PairingSession("ABC-123", DeviceRole.STATION, PairingStatus.WAITING)),
    var pairResult: DomainResult<PairingSession> =
        DomainResult.Success(PairingSession("ABC-123", DeviceRole.OBSERVER, PairingStatus.PAIRED)),
) : PairingRepository {
    var lastPairedKey: String? = null

    override suspend fun createKey() = createResult

    override suspend fun pair(key: String): DomainResult<PairingSession> {
        lastPairedKey = key
        return pairResult
    }
}
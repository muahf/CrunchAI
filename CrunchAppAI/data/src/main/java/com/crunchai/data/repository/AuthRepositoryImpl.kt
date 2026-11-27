package com.crunchai.data.repository

import com.crunchai.data.local.datastore.SessionPreferencesDataSource
import com.crunchai.data.remote.api.CrunchApi
import com.crunchai.data.remote.dto.AuthRequestDto
import com.crunchai.data.remote.dto.AuthResponseDto
import com.crunchai.data.remote.network.safeApiCall
import com.example.crunchai.domain.common.DomainResult
import com.example.crunchai.domain.model.DeviceRole
import com.example.crunchai.domain.model.UserSession
import com.example.crunchai.domain.repository.AuthRepository
import javax.inject.Inject

class AuthRepositoryImpl
    @Inject
    constructor(
        private val api: CrunchApi,
        private val session: SessionPreferencesDataSource,
    ) : AuthRepository {
        override suspend fun signUp(
            email: String,
            password: String,
            role: DeviceRole,
            deviceName: String,
        ): DomainResult<UserSession> = auth(role) { api.signUp(AuthRequestDto(email, password, role.name.lowercase(), deviceName)) }

        override suspend fun logIn(
            email: String,
            password: String,
            role: DeviceRole,
            deviceName: String,
        ): DomainResult<UserSession> = auth(role) { api.logIn(AuthRequestDto(email, password, role.name.lowercase(), deviceName)) }

        private suspend fun auth(
            role: DeviceRole,
            call: suspend () -> AuthResponseDto,
        ): DomainResult<UserSession> {
            val result = safeApiCall { call() }
            if (result is DomainResult.Success) {
                val dto = result.value
                session.saveAuth(token = dto.token, role = role)
                return DomainResult.Success(
                    UserSession(
                        userId = dto.user.id,
                        email = dto.user.email,
                        deviceId = dto.device.id,
                        role = role,
                        token = dto.token,
                    ),
                )
            }
            return result as DomainResult.Failure
        }
    }

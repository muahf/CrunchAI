package com.example.crunchai.domain.repository

import com.example.crunchai.domain.common.DomainResult
import com.example.crunchai.domain.model.DeviceRole
import com.example.crunchai.domain.model.UserSession

interface AuthRepository {
    suspend fun signUp(email: String, password: String, role: DeviceRole, deviceName: String): DomainResult<UserSession>

    suspend fun logIn(email: String, password: String, role: DeviceRole, deviceName: String): DomainResult<UserSession>
}
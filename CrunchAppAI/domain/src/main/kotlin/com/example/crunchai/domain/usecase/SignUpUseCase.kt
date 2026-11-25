package com.example.crunchai.domain.usecase

import com.example.crunchai.domain.common.DomainResult
import com.example.crunchai.domain.model.DeviceRole
import com.example.crunchai.domain.model.UserSession
import com.example.crunchai.domain.repository.AuthRepository
import javax.inject.Inject

class SignUpUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(
        email: String,
        password: String,
        repeatPassword: String,
        role: DeviceRole,
        deviceName: String,
    ): DomainResult<UserSession> {
        CredentialsValidator.validateEmail(email)?.let { return DomainResult.Failure(it) }
        CredentialsValidator.validatePassword(password)?.let { return DomainResult.Failure(it) }
        if (password != repeatPassword) {
            return DomainResult.Failure(com.example.crunchai.domain.common.DomainError.Validation("repeatPassword", "Пароли не совпадают"))
        }
        return repository.signUp(email.trim().lowercase(), password, role, deviceName)
    }
}
package com.crunchai.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crunchai.presentation.common.toUserMessage
import com.example.crunchai.domain.common.DomainError
import com.example.crunchai.domain.common.DomainResult
import com.example.crunchai.domain.model.DeviceRole
import com.example.crunchai.domain.usecase.LogInUseCase
import com.example.crunchai.domain.usecase.SignUpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel
@Inject
constructor(
    private val logIn: LogInUseCase,
    private val signUp: SignUpUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, fieldErrors = it.fieldErrors - "email", formError = null) }

    fun onPasswordChange(value: String) =
        _state.update { it.copy(password = value, fieldErrors = it.fieldErrors - "password", formError = null) }

    fun onRepeatPasswordChange(value: String) =
        _state.update { it.copy(repeatPassword = value, fieldErrors = it.fieldErrors - "repeatPassword", formError = null) }

    fun onRoleChange(role: DeviceRole) = _state.update { it.copy(role = role) }

    fun onModeChange(mode: AuthMode) = _state.update { it.copy(mode = mode, fieldErrors = emptyMap(), formError = null) }

    fun submit(deviceName: String) {
        val s = _state.value
        if (s.isLoading) return
        _state.update { it.copy(isLoading = true, fieldErrors = emptyMap(), formError = null) }
        viewModelScope.launch {
            val result =
                if (s.mode == AuthMode.LOG_IN) {
                    logIn(s.email, s.password, s.role, deviceName)
                } else {
                    signUp(s.email, s.password, s.repeatPassword, s.role, deviceName)
                }
            when (result) {
                is DomainResult.Success -> _state.update { it.copy(isLoading = false, isAuthenticated = true) }
                is DomainResult.Failure -> _state.update { it.withError(result.error, s.mode) }
            }
        }
    }

    private fun AuthUiState.withError(
        error: DomainError,
        mode: AuthMode,
    ): AuthUiState =
        when {
            error is DomainError.Validation && error.field in FIELDS ->
                copy(isLoading = false, fieldErrors = mapOf(error.field to error.reason))
            error is DomainError.Unauthorized && mode == AuthMode.LOG_IN ->
                copy(isLoading = false, formError = "Неверный email или пароль")
            error is DomainError.Conflict && mode == AuthMode.SIGN_UP ->
                copy(isLoading = false, formError = "Аккаунт с таким email уже есть. Войдите")
            else -> copy(isLoading = false, formError = error.toUserMessage())
        }

    private companion object {
        val FIELDS = setOf("email", "password", "repeatPassword")
    }
}
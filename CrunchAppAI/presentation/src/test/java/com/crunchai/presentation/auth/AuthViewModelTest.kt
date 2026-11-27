package com.crunchai.presentation.auth

import com.crunchai.presentation.MainDispatcherRule
import com.example.crunchai.domain.common.DomainError
import com.example.crunchai.domain.common.DomainResult
import com.example.crunchai.domain.model.DeviceRole
import com.example.crunchai.domain.model.UserSession
import com.example.crunchai.domain.repository.AuthRepository
import com.example.crunchai.domain.usecase.LogInUseCase
import com.example.crunchai.domain.usecase.SignUpUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private class FakeAuthRepo(var result: DomainResult<UserSession>) : AuthRepository {
    var calls = 0

    override suspend fun signUp(
        email: String,
        password: String,
        role: DeviceRole,
        deviceName: String,
    ): DomainResult<UserSession> {
        calls++
        return result
    }

    override suspend fun logIn(
        email: String,
        password: String,
        role: DeviceRole,
        deviceName: String,
    ): DomainResult<UserSession> {
        calls++
        return result
    }
}

class AuthViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val ok = DomainResult.Success(UserSession("u", "a@b.ru", "d", DeviceRole.OBSERVER, "t"))

    private fun vm(repo: FakeAuthRepo) = AuthViewModel(LogInUseCase(repo), SignUpUseCase(repo))

    @Test
    fun `invalid email shows field error and does not call server`() {
        val repo = FakeAuthRepo(ok)
        val vm = vm(repo)
        vm.onEmailChange("не email")
        vm.onPasswordChange("12345678")
        vm.submit("Pixel")
        assertEquals("Введите email вида name@mail.ru", vm.state.value.fieldErrors["email"])
        assertEquals(0, repo.calls)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `successful login sets authenticated`() {
        val vm = vm(FakeAuthRepo(ok))
        vm.onEmailChange("a@b.ru")
        vm.onPasswordChange("12345678")
        vm.submit("Pixel")
        assertTrue(vm.state.value.isAuthenticated)
        assertNull(vm.state.value.formError)
    }

    @Test
    fun `wrong password shows readable message`() {
        val vm = vm(FakeAuthRepo(DomainResult.Failure(DomainError.Unauthorized)))
        vm.onEmailChange("a@b.ru")
        vm.onPasswordChange("12345678")
        vm.submit("Pixel")
        assertEquals("Неверный email или пароль", vm.state.value.formError)
        assertFalse(vm.state.value.isAuthenticated)
    }

    @Test
    fun `email already taken on sign up`() {
        val vm = vm(FakeAuthRepo(DomainResult.Failure(DomainError.Conflict)))
        vm.onModeChange(AuthMode.SIGN_UP)
        vm.onEmailChange("a@b.ru")
        vm.onPasswordChange("12345678")
        vm.onRepeatPasswordChange("12345678")
        vm.submit("Pixel")
        assertEquals("Аккаунт с таким email уже есть. Войдите", vm.state.value.formError)
    }

    @Test
    fun `different passwords on sign up`() {
        val repo = FakeAuthRepo(ok)
        val vm = vm(repo)
        vm.onModeChange(AuthMode.SIGN_UP)
        vm.onEmailChange("a@b.ru")
        vm.onPasswordChange("12345678")
        vm.onRepeatPasswordChange("87654321")
        vm.submit("Pixel")
        assertEquals("Пароли не совпадают", vm.state.value.fieldErrors["repeatPassword"])
        assertEquals(0, repo.calls)
    }

    @Test
    fun `editing a field clears its error`() {
        val vm = vm(FakeAuthRepo(ok))
        vm.onEmailChange("bad")
        vm.onPasswordChange("12345678")
        vm.submit("Pixel")
        vm.onEmailChange("a@b.ru")
        assertNull(vm.state.value.fieldErrors["email"])
    }

    @Test
    fun `switching mode clears errors`() {
        val vm = vm(FakeAuthRepo(DomainResult.Failure(DomainError.Unauthorized)))
        vm.onEmailChange("a@b.ru")
        vm.onPasswordChange("12345678")
        vm.submit("Pixel")
        vm.onModeChange(AuthMode.SIGN_UP)
        assertNull(vm.state.value.formError)
        assertEquals(AuthMode.SIGN_UP, vm.state.value.mode)
    }

    @Test
    fun `offline error is shown as message`() {
        val vm = vm(FakeAuthRepo(DomainResult.Failure(DomainError.Offline)))
        vm.onEmailChange("a@b.ru")
        vm.onPasswordChange("12345678")
        vm.submit("Pixel")
        assertTrue(vm.state.value.formError!!.isNotBlank())
    }
}

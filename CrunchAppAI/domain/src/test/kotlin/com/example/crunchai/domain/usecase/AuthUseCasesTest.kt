package com.example.crunchai.domain.usecase

import com.example.crunchai.domain.common.DomainError
import com.example.crunchai.domain.common.DomainResult
import com.example.crunchai.domain.model.DeviceRole
import com.example.crunchai.domain.model.UserSession
import com.example.crunchai.domain.repository.AuthRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeAuthRepository : AuthRepository {
    var calls = 0
    var lastEmail: String? = null

    private fun ok(email: String, role: DeviceRole) =
        DomainResult.Success(UserSession("u1", email, "d1", role, "token"))

    override suspend fun signUp(email: String, password: String, role: DeviceRole, deviceName: String): DomainResult<UserSession> {
        calls++
        lastEmail = email
        return ok(email, role)
    }

    override suspend fun logIn(email: String, password: String, role: DeviceRole, deviceName: String): DomainResult<UserSession> {
        calls++
        lastEmail = email
        return ok(email, role)
    }
}

class AuthUseCasesTest {
    private val repo = FakeAuthRepository()
    private val logIn = LogInUseCase(repo)
    private val signUp = SignUpUseCase(repo)

    private fun error(r: DomainResult<UserSession>) = (r as DomainResult.Failure).error as DomainError.Validation

    @Test
    fun `empty email is rejected without calling server`() = runBlocking {
        val r = logIn(" ", "12345678", DeviceRole.OBSERVER, "Pixel")
        assertEquals("email", error(r).field)
        assertEquals(0, repo.calls)
    }

    @Test
    fun `email without domain is rejected`() = runBlocking {
        assertEquals("email", error(logIn("anna@mail", "12345678", DeviceRole.OBSERVER, "Pixel")).field)
    }

    @Test
    fun `short password is rejected`() = runBlocking {
        assertEquals("password", error(logIn("a@b.ru", "1234567", DeviceRole.OBSERVER, "Pixel")).field)
        assertEquals(0, repo.calls)
    }

    @Test
    fun `too long password is rejected`() = runBlocking {
        assertEquals("password", error(logIn("a@b.ru", "x".repeat(73), DeviceRole.OBSERVER, "Pixel")).field)
    }

    @Test
    fun `valid login trims and lowercases email`() = runBlocking {
        val r = logIn("  Anna@Mail.RU ", "12345678", DeviceRole.OBSERVER, "Pixel")
        assertTrue(r is DomainResult.Success)
        assertEquals("anna@mail.ru", repo.lastEmail)
    }

    @Test
    fun `sign up with different passwords is rejected`() = runBlocking {
        val r = signUp("a@b.ru", "12345678", "12345679", DeviceRole.OBSERVER, "Pixel")
        assertEquals("repeatPassword", error(r).field)
        assertEquals(0, repo.calls)
    }

    @Test
    fun `valid sign up calls repository once`() = runBlocking {
        val r = signUp("a@b.ru", "12345678", "12345678", DeviceRole.STATION, "Кухня")
        assertTrue(r is DomainResult.Success)
        assertEquals(1, repo.calls)
    }

    @Test
    fun `validator accepts good email and password`() {
        assertNull(CredentialsValidator.validateEmail("a@b.ru"))
        assertNull(CredentialsValidator.validatePassword("12345678"))
    }
}
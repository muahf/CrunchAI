package com.crunchai.presentation.auth

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crunchai.domain.model.DeviceRole

@Composable
fun AuthRoute(
    onAuthenticated: (DeviceRole) -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.isAuthenticated) {
        if (state.isAuthenticated) {
            onAuthenticated(state.role)
        }
    }
    AuthScreen(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onRepeatPasswordChange = viewModel::onRepeatPasswordChange,
        onRoleChange = viewModel::onRoleChange,
        onModeChange = viewModel::onModeChange,
        onSubmit = { viewModel.submit(Build.MODEL ?: "Телефон") },
    )
}

@Composable
fun AuthScreen(
    state: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRepeatPasswordChange: (String) -> Unit,
    onRoleChange: (DeviceRole) -> Unit,
    onModeChange: (AuthMode) -> Unit,
    onSubmit: () -> Unit,
) {
    val isSignUp = state.mode == AuthMode.SIGN_UP
    Scaffold { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = if (isSignUp) "Создать аккаунт" else "Вход",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "Роль этого телефона",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.role == DeviceRole.OBSERVER,
                    onClick = { onRoleChange(DeviceRole.OBSERVER) },
                    label = { Text("Наблюдатель") },
                )
                FilterChip(
                    selected = state.role == DeviceRole.STATION,
                    onClick = { onRoleChange(DeviceRole.STATION) },
                    label = { Text("Станция") },
                )
            }

            AuthField("Email", state.email, onEmailChange, state.fieldErrors["email"], KeyboardType.Email, "email")
            AuthField("Пароль", state.password, onPasswordChange, state.fieldErrors["password"], KeyboardType.Password, "password", true)
            if (isSignUp) {
                AuthField(
                    "Повторите пароль",
                    state.repeatPassword,
                    onRepeatPasswordChange,
                    state.fieldErrors["repeatPassword"],
                    KeyboardType.Password,
                    "repeatPassword",
                    true,
                )
            }

            state.formError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("formError"),
                )
            }

            Button(
                onClick = onSubmit,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("submit"),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (isSignUp) "Зарегистрироваться" else "Войти")
                }
            }
            Spacer(Modifier.height(4.dp))
            TextButton(
                onClick = { onModeChange(if (isSignUp) AuthMode.LOG_IN else AuthMode.SIGN_UP) },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(if (isSignUp) "Уже есть аккаунт? Войти" else "Нет аккаунта? Зарегистрироваться")
            }
        }
    }
}

@Composable
private fun AuthField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    error: String?,
    keyboard: KeyboardType,
    tag: String,
    isPassword: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = Modifier.fillMaxWidth().testTag(tag),
    )
}

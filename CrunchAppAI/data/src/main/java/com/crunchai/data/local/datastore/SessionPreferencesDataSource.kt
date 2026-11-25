package com.crunchai.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.crunchai.data.remote.interceptor.TokenProvider
import com.example.crunchai.domain.model.DeviceRole
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "crunch_session_prefs")

@Singleton
class SessionPreferencesDataSource
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : TokenProvider {
        private object Keys {
            val AUTH_TOKEN = stringPreferencesKey("auth_token")
            val DEVICE_ROLE = stringPreferencesKey("device_role")
            val PAIR_KEY = stringPreferencesKey("pair_key")
        }

        val deviceRoleFlow: Flow<DeviceRole?> =
            context.sessionDataStore.data.map { prefs ->
                prefs[Keys.DEVICE_ROLE]?.let { runCatching { DeviceRole.valueOf(it) }.getOrNull() }
            }

        val pairKeyFlow: Flow<String?> =
            context.sessionDataStore.data.map { prefs ->
                prefs[Keys.PAIR_KEY]
            }

        override fun getToken(): String? =
            runBlocking {
                context.sessionDataStore.data.first()[Keys.AUTH_TOKEN]
            }

        suspend fun saveSession(
            token: String?,
            role: DeviceRole,
            pairKey: String,
        ) {
            context.sessionDataStore.edit { prefs ->
                if (token != null) prefs[Keys.AUTH_TOKEN] = token else prefs.remove(Keys.AUTH_TOKEN)
                prefs[Keys.DEVICE_ROLE] = role.name
                prefs[Keys.PAIR_KEY] = pairKey
            }
        }
    /** Вход выполнен: запоминаем токен и роль. Ключ привязки (если уже был) не трогаем. */
    suspend fun saveAuth(
        token: String,
        role: DeviceRole,
    ) {
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.AUTH_TOKEN] = token
            prefs[Keys.DEVICE_ROLE] = role.name
        }
    }

    /** Есть ли сохранённый токен (для решения «показывать вход или главную»). */
    val isLoggedInFlow: Flow<Boolean> =
        context.sessionDataStore.data.map { prefs -> prefs[Keys.AUTH_TOKEN] != null }
        suspend fun clear() {
            context.sessionDataStore.edit { it.clear() }
        }
    }

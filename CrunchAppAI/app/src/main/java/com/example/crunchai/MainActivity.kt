package com.example.crunchai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.crunchai.presentation.navigation.CrunchNavGraph
import com.crunchai.presentation.ui.theme.CrunchAiTheme
import com.crunchai.presentation.ui.theme.LocalThemeController
import com.crunchai.presentation.ui.theme.ThemeController
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val systemDark = isSystemInDarkTheme()
            var userDark by rememberSaveable { mutableStateOf<Boolean?>(null) } // null = как в системе
            val isDark = userDark ?: systemDark
            val controller = remember(isDark) { ThemeController(isDark) { userDark = !isDark } }

            CompositionLocalProvider(LocalThemeController provides controller) {
                CrunchAiTheme(darkTheme = isDark) {
                    CrunchNavGraph() // твоё текущее содержимое оставь как есть
                }
            }
        }
    }
}
package com.example.crunchai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.crunchai.ui.components.ComponentsShowcase
import com.example.crunchai.ui.theme.CrunchAiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val systemDark = isSystemInDarkTheme()
            var dark by rememberSaveable { mutableStateOf(systemDark) }
            var dynamic by rememberSaveable { mutableStateOf(false) }

            CrunchAiTheme(darkTheme = dark, dynamicColor = dynamic) {
                ComponentsShowcase(
                    darkTheme = dark,
                    dynamicColor = dynamic,
                    onDarkThemeChange = { dark = it },
                    onDynamicColorChange = { dynamic = it },
                )
            }
        }
    }
}
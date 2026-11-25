package com.crunchai.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.crunchai.presentation.R

enum class MainTab { Home, Diary, Pet }

/** Нижняя панель: Главная · Дневник · Питомец. Высота 72, выбранный пункт с «таблеткой» 56×28. */
@Composable
fun AppBottomBar(
    selected: MainTab,
    onHome: () -> Unit,
    onDiary: () -> Unit,
    onPet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .navigationBarsPadding()
                .height(72.dp),
    ) {
        BottomItem("Главная", R.drawable.home, selected == MainTab.Home, onHome, Modifier.weight(1f))
        BottomItem("Дневник", R.drawable.diary, selected == MainTab.Diary, onDiary, Modifier.weight(1f))
        BottomItem("Питомец", R.drawable.paw, selected == MainTab.Pet, onPet, Modifier.weight(1f))
    }
}

@Composable
private fun BottomItem(
    label: String,
    icon: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier.height(72.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .width(56.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) colors.primaryContainer else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) colors.onSurface else colors.onSurfaceVariant,
        )
    }
}
package com.crunchai.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.crunchai.presentation.common.formatTime
import com.example.crunchai.domain.model.Meal
import java.time.ZoneId

/** Одна строка списка приёмов пищи: время, длительность и индекс спокойствия. */
@Composable
fun MealRow(
    meal: Meal,
    zone: ZoneId,
    modifier: Modifier = Modifier,
) {
    val minutes = (meal.durationSeconds / 60).coerceAtLeast(1)
    ContentCard(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(formatTime(meal.startedAt, zone), style = MaterialTheme.typography.labelLarge)
                Text(
                    "$minutes мин",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("Спокойствие ${meal.calmIndex}/10", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
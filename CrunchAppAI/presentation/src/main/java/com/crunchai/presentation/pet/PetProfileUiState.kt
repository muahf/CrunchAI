package com.crunchai.presentation.pet

import com.example.crunchai.domain.model.Species

data class PetProfileUiState(
    val petId: String? = null,
    val name: String = "",
    val species: Species? = null,
    val ageYearsInput: String = "",
    val weightKgInput: String = "",
    val bodyScore: Int? = null,
    val photoUri: String? = null,
    val isSaving: Boolean = false,
    val isOffline: Boolean = false,
    val serverError: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
    val saveSuccess: Boolean = false,
) {
    val bodyScoreDescription: String?
        get() =
            when (bodyScore) {
                1, 2 -> "$bodyScore — сильное истощение: рёбра и позвонки резко выступают"
                3, 4 -> "$bodyScore — недостаточный вес: рёбра легко прощупываются без жировой прослойки"
                5 -> "5 — идеальная форма: рёбра прощупываются, талия заметна"
                6, 7 -> "$bodyScore — избыточный вес: рёбра прощупываются с трудом, талия сглажена"
                8, 9 -> "$bodyScore — ожирение: выраженные жировые отложения, талия отсутствует"
                else -> null
            }
}

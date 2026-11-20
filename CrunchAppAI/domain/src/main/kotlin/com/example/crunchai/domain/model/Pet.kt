package com.example.crunchai.domain.model

enum class Species { DOG, CAT, OTHER }

/** Питомец и его профиль. Необязательные поля пустые, пока владелец их не заполнил. */
data class Pet(
    val id: String,
    val name: String,
    val species: Species,
    val ageMonths: Int? = null,
    val weightKg: Double? = null,
    val bodyScore: Int? = null,   // оценка кондиции 1..9
)
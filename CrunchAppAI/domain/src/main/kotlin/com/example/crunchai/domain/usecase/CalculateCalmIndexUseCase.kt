package com.example.crunchai.domain.usecase

/** Индекс спокойствия еды от 1 до 10: чем больше кашля во время еды, тем он ниже. */
class CalculateCalmIndexUseCase {
    operator fun invoke(coughCount: Int): Int = (MAX_INDEX - coughCount).coerceIn(MIN_INDEX, MAX_INDEX)

    private companion object {
        const val MIN_INDEX = 1
        const val MAX_INDEX = 10
    }
}
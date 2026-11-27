package com.crunchai.presentation.pet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crunchai.presentation.common.toUserMessage
import com.example.crunchai.domain.common.DomainError
import com.example.crunchai.domain.common.DomainResult
import com.example.crunchai.domain.model.Pet
import com.example.crunchai.domain.model.Species
import com.example.crunchai.domain.usecase.ObservePetUseCase
import com.example.crunchai.domain.usecase.SavePetProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class PetProfileViewModel
    @Inject
    constructor(
        private val observePet: ObservePetUseCase,
        private val savePetProfile: SavePetProfileUseCase,
    ) : ViewModel() {
        private val _state = MutableStateFlow(PetProfileUiState())
        val state: StateFlow<PetProfileUiState> = _state.asStateFlow()

        private var initializedFromDb = false

        init {
            viewModelScope.launch {
                observePet().collect { pet ->
                    if (pet != null && !initializedFromDb) {
                        initializedFromDb = true
                        _state.update {
                            it.copy(
                                petId = pet.id,
                                name = pet.name,
                                species = pet.species,
                                ageYearsInput = pet.ageMonths?.let { m -> formatYearsFromMonths(m) } ?: "",
                                weightKgInput = pet.weightKg?.let { w -> String.format(Locale.US, "%.1f", w) } ?: "",
                                bodyScore = pet.bodyScore,
                            )
                        }
                    }
                }
            }
        }

        fun onNameChange(value: String) =
            _state.update { it.copy(name = value, fieldErrors = it.fieldErrors - "name", serverError = null, saveSuccess = false) }

        fun onSpeciesChange(species: Species) =
            _state.update { it.copy(species = species, fieldErrors = it.fieldErrors - "species", serverError = null, saveSuccess = false) }

        fun onAgeChange(value: String) =
            _state.update {
                it.copy(
                    ageYearsInput = value,
                    fieldErrors = it.fieldErrors - "ageMonths",
                    serverError = null,
                    saveSuccess = false,
                )
            }

        fun onWeightChange(value: String) =
            _state.update {
                it.copy(
                    weightKgInput = value,
                    fieldErrors = it.fieldErrors - "weightKg",
                    serverError = null,
                    saveSuccess = false,
                )
            }

        fun onBodyScoreChange(score: Int) =
            _state.update { it.copy(bodyScore = if (it.bodyScore == score) null else score, serverError = null, saveSuccess = false) }

        fun onPhotoSelected(uri: String) = _state.update { it.copy(photoUri = uri, saveSuccess = false) }

        fun save() {
            val s = _state.value
            if (s.isSaving) return

            val errors = mutableMapOf<String, String>()
            val selectedSpecies = s.species
            if (selectedSpecies == null) {
                errors["species"] = "Выберите вид питомца"
            }

            val parsedAgeMonths = parseAgeToMonths(s.ageYearsInput)
            if (s.ageYearsInput.isNotBlank() && parsedAgeMonths == null) {
                errors["ageMonths"] = "Введите возраст числом (например, 3 или 1.5)"
            }

            val parsedWeight = parseWeightKg(s.weightKgInput)
            if (s.weightKgInput.isNotBlank() && parsedWeight == null) {
                errors["weightKg"] = "Введите вес числом (например, 18.4)"
            }

            if (errors.isNotEmpty() || selectedSpecies == null) {
                _state.update { it.copy(fieldErrors = errors) }
                return
            }

            val pet =
                Pet(
                    id = s.petId ?: UUID.randomUUID().toString(),
                    name = s.name,
                    species = selectedSpecies,
                    ageMonths = parsedAgeMonths,
                    weightKg = parsedWeight,
                    bodyScore = s.bodyScore,
                )

            _state.update { it.copy(isSaving = true, fieldErrors = emptyMap(), serverError = null, saveSuccess = false) }

            viewModelScope.launch {
                when (val result = savePetProfile(pet)) {
                    is DomainResult.Success -> {
                        _state.update {
                            it.copy(
                                petId = pet.id,
                                isSaving = false,
                                isOffline = false,
                                serverError = null,
                                saveSuccess = true,
                            )
                        }
                    }
                    is DomainResult.Failure -> {
                        val err = result.error
                        when {
                            err is DomainError.Validation ->
                                _state.update {
                                    it.copy(isSaving = false, fieldErrors = mapOf(err.field to err.reason))
                                }
                            err is DomainError.Offline ->
                                _state.update {
                                    it.copy(isSaving = false, isOffline = true)
                                }
                            else ->
                                _state.update {
                                    it.copy(
                                        isSaving = false,
                                        serverError =
                                            "Не удалось сохранить: ${err.toUserMessage().lowercase()}. " +
                                                "Данные остались на телефоне.",
                                    )
                                }
                        }
                    }
                }
            }
        }

        private fun parseAgeToMonths(raw: String): Int? {
            val cleaned = raw.trim().replace(",", ".").replace(Regex("[^0-9.]"), "")
            if (cleaned.isEmpty()) return null
            val years = cleaned.toDoubleOrNull() ?: return null
            return (years * 12.0).roundToInt()
        }

        private fun parseWeightKg(raw: String): Double? {
            val cleaned = raw.trim().replace(",", ".").replace(Regex("[^0-9.]"), "")
            if (cleaned.isEmpty()) return null
            return cleaned.toDoubleOrNull()
        }

        private fun formatYearsFromMonths(months: Int): String =
            if (months % 12 == 0) {
                "${months / 12}"
            } else {
                String.format(Locale.US, "%.1f", months / 12.0)
            }
    }

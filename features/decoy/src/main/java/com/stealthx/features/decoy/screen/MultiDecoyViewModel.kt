/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.features.decoy.screen

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stealthx.data.prefs.AppPreferences
import com.stealthx.domain.tier.TierGate
import com.stealthx.features.decoy.engine.DecoyProfileEngine
import com.stealthx.shared.model.IfrTier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject

data class MultiDecoyUiState(
    val profiles: List<DecoyProfileEntry> = emptyList(),
    val isSaving: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val storeCorrupted: Boolean = false
)

data class DecoyProfileEntry(
    val id: String,
    val name: String,
    val pinHashBase64: String,
    val pinSaltBase64: String
)

@HiltViewModel
class MultiDecoyViewModel @Inject constructor(
    private val engine: DecoyProfileEngine,
    private val tierGate: TierGate,
    private val prefs: AppPreferences
) : ViewModel() {

    private val _uiState: MutableStateFlow<MultiDecoyUiState>

    init {
        val (profiles, corrupted) = loadProfilesWithStatus()
        _uiState = MutableStateFlow(
            MultiDecoyUiState(
                profiles = profiles,
                storeCorrupted = corrupted,
                errorMessage = if (corrupted) "Profile store was corrupted and has been reset" else null
            )
        )
    }

    val uiState: StateFlow<MultiDecoyUiState> = _uiState.asStateFlow()

    fun addProfile(
        name: String,
        realPin: String,
        decoyPin: String,
        confirmDecoyPin: String,
        onAdded: () -> Unit = {}
    ) {
        // Validate synchronously for immediate UX feedback
        val validationError = validate(name, realPin, decoyPin, confirmDecoyPin)
        if (validationError != null) {
            _uiState.value = _uiState.value.copy(errorMessage = validationError, statusMessage = null)
            return
        }

        val realSaltB64 = prefs.realPinSaltBase64
        val realHashB64 = prefs.realPinHashBase64
        if (realSaltB64 == null || realHashB64 == null) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Configure your Decoy Profile first before adding multiple profiles",
                statusMessage = null
            )
            return
        }

        _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null, statusMessage = null)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Tier check uses suspend getTier() for accurate result, not stale cache
                if (tierGate.getTier() < IfrTier.ELITE) {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = "Elite tier required for Multi-Decoy Profiles"
                    )
                    return@launch
                }

                val realSalt = Base64.decode(realSaltB64, Base64.NO_WRAP)
                val realHash = Base64.decode(realHashB64, Base64.NO_WRAP)
                val realDerived = engine.hashPin(realPin, realSalt)

                if (!realDerived.contentEquals(realHash)) {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = "Real PIN is incorrect"
                    )
                    return@launch
                }

                val existing = _uiState.value.profiles
                for (entry in existing) {
                    val salt = Base64.decode(entry.pinSaltBase64, Base64.NO_WRAP)
                    val hash = Base64.decode(entry.pinHashBase64, Base64.NO_WRAP)
                    if (engine.hashPin(decoyPin, salt).contentEquals(hash)) {
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            errorMessage = "This PIN is already used by '${entry.name}'"
                        )
                        return@launch
                    }
                }

                val decoySalt = engine.generatePinSalt()
                val decoyHash = engine.hashPin(decoyPin, decoySalt)

                val newEntry = DecoyProfileEntry(
                    id = UUID.randomUUID().toString(),
                    name = name.trim(),
                    pinHashBase64 = decoyHash.toBase64(),
                    pinSaltBase64 = decoySalt.toBase64()
                )

                val updated = existing + newEntry
                saveProfiles(updated)
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    profiles = updated,
                    statusMessage = "Profile '${newEntry.name}' added",
                    errorMessage = null
                )
                withContext(Dispatchers.Main) { onAdded() }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = e.message ?: "Failed to add profile"
                )
            }
        }
    }

    fun removeProfile(id: String) {
        viewModelScope.launch {
            // suspend getTier() gives accurate tier, not potentially stale cache
            if (tierGate.getTier() < IfrTier.ELITE) {
                _uiState.value = _uiState.value.copy(errorMessage = "Elite tier required")
                return@launch
            }
            val updated = _uiState.value.profiles.filter { it.id != id }
            withContext(Dispatchers.IO) { saveProfiles(updated) }
            _uiState.value = _uiState.value.copy(
                profiles = updated,
                statusMessage = "Profile removed",
                errorMessage = null
            )
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(statusMessage = null, errorMessage = null)
    }

    private fun validate(name: String, realPin: String, decoyPin: String, confirm: String): String? {
        if (name.isBlank()) return "Profile name is required"
        if (realPin.length < MIN_PIN_LENGTH) return "Real PIN must be at least $MIN_PIN_LENGTH digits"
        if (decoyPin.length < MIN_PIN_LENGTH) return "Decoy PIN must be at least $MIN_PIN_LENGTH digits"
        if (!realPin.all(Char::isDigit) || !decoyPin.all(Char::isDigit)) return "PINs must contain digits only"
        if (realPin == decoyPin) return "Decoy PIN must differ from real PIN"
        if (decoyPin != confirm) return "Decoy PIN confirmation does not match"
        return null
    }

    private fun loadProfilesWithStatus(): Pair<List<DecoyProfileEntry>, Boolean> {
        return try {
            val arr = JSONArray(prefs.decoyProfilesJson)
            val profiles = (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                DecoyProfileEntry(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    pinHashBase64 = obj.getString("pinHash"),
                    pinSaltBase64 = obj.getString("pinSalt")
                )
            }
            Pair(profiles, false)
        } catch (_: Exception) {
            prefs.decoyProfilesJson = "[]"
            Pair(emptyList(), true)
        }
    }

    private fun saveProfiles(profiles: List<DecoyProfileEntry>) {
        val arr = JSONArray()
        profiles.forEach { entry ->
            arr.put(
                JSONObject().apply {
                    put("id", entry.id)
                    put("name", entry.name)
                    put("pinHash", entry.pinHashBase64)
                    put("pinSalt", entry.pinSaltBase64)
                }
            )
        }
        prefs.decoyProfilesJson = arr.toString()
    }

    private fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)

    private companion object {
        const val MIN_PIN_LENGTH = 4
    }
}

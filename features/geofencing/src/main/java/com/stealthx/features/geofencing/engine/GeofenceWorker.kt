/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.features.geofencing.engine

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.location.Geofence
import com.stealthx.data.prefs.AppPreferences
import com.stealthx.domain.repository.SecureRuleRepository
import com.stealthx.domain.rules.RuleEngine
import com.stealthx.domain.rules.TriggerType
import com.stealthx.domain.tier.TierGate
import com.stealthx.shared.model.IfrTier
import com.stealthx.shared.model.SecurityLevel
import com.stealthx.shared.model.TriggerContext
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

/**
 * GeofenceWorker — processes geofence transitions via WorkManager.
 *
 * Uses WorkManager instead of foreground service for battery efficiency.
 * Triggers RuleEngine with location context when geofence is entered/exited.
 */
class GeofenceWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val geofenceId = inputData.getString("geofence_id") ?: return Result.failure()
        val transitionType = inputData.getInt("transition_type", -1)
        val latitude = inputData.getDouble("latitude", 0.0)
        val longitude = inputData.getDouble("longitude", 0.0)

        if (!transitionType.isSupportedTransition()) return Result.failure()
        if (!latitude.isFinite() || !longitude.isFinite()) return Result.failure()

        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            GeofenceWorkerEntryPoint::class.java
        )

        if (entryPoint.tierGate().getTierSync() < IfrTier.ELITE) {
            entryPoint.appPreferences().defaultSecurityLevel = SecurityLevel.PROTECTED.name
            return Result.success()
        }

        val context = TriggerContext(latitude = latitude, longitude = longitude)
        val locationRules = entryPoint.secureRuleRepository()
            .observeEnabled()
            .first()
            .filter { it.triggerType == TriggerType.LOCATION }
        val matchedRules = entryPoint.ruleEngine().matchingRules(locationRules, context)
        val securityLevel = entryPoint.ruleEngine().resolveConflicts(matchedRules)
        val timestamp = System.currentTimeMillis() / 1000

        entryPoint.appPreferences().defaultSecurityLevel = securityLevel.name
        matchedRules.forEach { rule ->
            entryPoint.secureRuleRepository().recordTrigger(rule.id, timestamp)
        }

        return Result.success()
    }

    private fun Int.isSupportedTransition(): Boolean =
        this == Geofence.GEOFENCE_TRANSITION_ENTER ||
            this == Geofence.GEOFENCE_TRANSITION_EXIT ||
            this == Geofence.GEOFENCE_TRANSITION_DWELL

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface GeofenceWorkerEntryPoint {
        fun appPreferences(): AppPreferences
        fun ruleEngine(): RuleEngine
        fun secureRuleRepository(): SecureRuleRepository
        fun tierGate(): TierGate
    }
}

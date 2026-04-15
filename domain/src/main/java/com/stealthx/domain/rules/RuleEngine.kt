/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.domain.rules

import com.stealthx.shared.model.SecurityLevel
import com.stealthx.shared.model.TriggerContext
/**
 * Rule Engine — evaluates active rules against current context.
 *
 * Conflict Resolution (Fail Secure):
 *   When multiple rules match, the HIGHEST security level ALWAYS wins.
 *   There is no priority inversion. A user can temporarily override
 *   downward (e.g. "relax for 30 minutes") but never bypass upward.
 *
 * Default: If no rules match → PROTECTED (never PUBLIC by default).
 */
class RuleEngine {

    /**
     * Evaluate a list of active rules against the current context.
     * Returns the highest matching SecurityLevel.
     *
     * @param rules    Active rules from RuleRepository
     * @param context  Current trigger context (app, wifi, location, time)
     * @return         Resolved SecurityLevel (always the maximum)
     */
    fun evaluate(rules: List<SecureRule>, context: TriggerContext): SecurityLevel {
        val matchingRules = rules
            .filter { it.isEnabled }
            .filter { matches(it, context) }

        return resolveConflicts(matchingRules)
    }

    /**
     * Resolve conflicts: highest security level wins.
     * Default: PROTECTED (Fail Secure — never PUBLIC without explicit rule).
     */
    fun resolveConflicts(rules: List<SecureRule>): SecurityLevel {
        return rules
            .maxByOrNull { it.securityLevel.ordinal }
            ?.securityLevel
            ?: SecurityLevel.PROTECTED  // Fail Secure default
    }

    /**
     * Check if a rule matches the current context.
     */
    private fun matches(rule: SecureRule, context: TriggerContext): Boolean {
        return when (rule.triggerType) {
            TriggerType.APP ->
                context.packageName != null &&
                context.packageName == rule.triggerValue

            TriggerType.WIFI ->
                context.wifiSsid != null &&
                context.wifiSsid == rule.triggerValue

            TriggerType.LOCATION -> {
                val lat = context.latitude
                val lng = context.longitude
                lat != null && lng != null &&
                isWithinGeofence(lat, lng, rule.triggerValue)
            }

            TriggerType.TIME ->
                isWithinTimeWindow(context.hourOfDay, context.dayOfWeek, rule.triggerValue)

            TriggerType.BLUETOOTH ->
                context.bluetoothId != null &&
                context.bluetoothId == rule.triggerValue
        }
    }

    private fun isWithinGeofence(lat: Double, lng: Double, geoJson: String): Boolean {
        // TODO: parse JSON {"lat":X, "lng":Y, "radius":Z} and compute distance
        // Implement in S-08 Geofencing feature
        return false
    }

    private fun isWithinTimeWindow(hour: Int, day: Int, timeJson: String): Boolean {
        // TODO: parse JSON {"startHour":X, "endHour":Y, "days":[1,2,3,4,5]}
        // Implement in S-05
        return false
    }
}

/**
 * Domain model for a secure rule.
 * Stored in Room via :data layer.
 */
data class SecureRule(
    val id:            String,          // UUID as String
    val name:          String,
    val triggerType:   TriggerType,
    val triggerValue:  String,          // JSON-encoded trigger parameters
    val actionType:    ActionType,
    val securityLevel: SecurityLevel,
    val priority:      Int,             // Higher = more important (for future use)
    val isEnabled:     Boolean,
    val createdAt:     Long,            // epoch seconds
    val lastTriggered: Long?,
    val triggerCount:  Int
)

enum class TriggerType { APP, LOCATION, TIME, WIFI, BLUETOOTH }
enum class ActionType  { SET_LEVEL, AUTO_ENCRYPT, SHOW_DECOY }

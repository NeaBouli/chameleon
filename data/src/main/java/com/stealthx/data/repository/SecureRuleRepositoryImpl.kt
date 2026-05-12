/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.data.repository

import com.stealthx.data.dao.SecureRuleDao
import com.stealthx.data.entity.SecureRuleEntity
import com.stealthx.domain.repository.SecureRuleRepository
import com.stealthx.domain.rules.ActionType
import com.stealthx.domain.rules.SecureRule
import com.stealthx.domain.rules.TriggerType
import com.stealthx.shared.model.SecurityLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SecureRuleRepositoryImpl(
    private val dao: SecureRuleDao
) : SecureRuleRepository {

    override fun observeEnabled(): Flow<List<SecureRule>> =
        dao.observeEnabled().map { rules -> rules.map { it.toDomain() } }

    override fun observeAll(): Flow<List<SecureRule>> =
        dao.observeAll().map { rules -> rules.map { it.toDomain() } }

    override suspend fun getById(id: String): SecureRule? =
        dao.getById(id)?.toDomain()

    override suspend fun save(rule: SecureRule) {
        dao.upsert(rule.toEntity())
    }

    override suspend fun delete(rule: SecureRule) {
        dao.delete(rule.toEntity())
    }

    override suspend fun deleteAll() {
        dao.deleteAll()
    }

    override suspend fun recordTrigger(ruleId: String, timestamp: Long) {
        dao.recordTrigger(ruleId, timestamp)
    }

    private fun SecureRuleEntity.toDomain(): SecureRule =
        SecureRule(
            id = id,
            name = name,
            triggerType = triggerType.toEnum(TriggerType.APP),
            triggerValue = triggerValue,
            actionType = actionType.toEnum(ActionType.SET_LEVEL),
            securityLevel = securityLevel.toEnum(SecurityLevel.PROTECTED),
            priority = priority,
            isEnabled = isEnabled,
            createdAt = createdAt,
            lastTriggered = lastTriggered,
            triggerCount = triggerCount
        )

    private fun SecureRule.toEntity(): SecureRuleEntity =
        SecureRuleEntity(
            id = id,
            name = name,
            triggerType = triggerType.name,
            triggerValue = triggerValue,
            actionType = actionType.name,
            securityLevel = securityLevel.name,
            priority = priority,
            isEnabled = isEnabled,
            createdAt = createdAt,
            lastTriggered = lastTriggered,
            triggerCount = triggerCount
        )

    private inline fun <reified T : Enum<T>> String.toEnum(default: T): T =
        enumValues<T>().firstOrNull { it.name == this } ?: default
}

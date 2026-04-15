/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.chameleon.di

import android.content.Context
import com.stealthx.data.ChameleonDatabase
import com.stealthx.data.dao.AuditLogDao
import com.stealthx.data.dao.CryptoKeyDao
import com.stealthx.data.dao.IfrTierCacheDao
import com.stealthx.data.dao.SecureRuleDao
import com.stealthx.data.repository.IfrTierRepositoryImpl
import com.stealthx.domain.repository.IfrTierRepository
import com.stealthx.domain.repository.SecureRuleRepository
import com.stealthx.domain.rules.SecureRule
import com.stealthx.domain.tier.TierGate
import com.stealthx.domain.tier.TierGateImpl
import com.stealthx.security.KeystoreManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        keystoreManager: KeystoreManager
    ): ChameleonDatabase {
        // Generate DB passphrase from Keystore AES key
        val aesKey = keystoreManager.getOrCreateAesKey("chameleon_db_key", requireAuth = false)
        val passphrase = aesKey.encoded ?: ByteArray(32) { 0 }
        return ChameleonDatabase.build(context, passphrase)
    }

    @Provides
    fun provideSecureRuleDao(db: ChameleonDatabase): SecureRuleDao = db.secureRuleDao()

    @Provides
    fun provideCryptoKeyDao(db: ChameleonDatabase): CryptoKeyDao = db.cryptoKeyDao()

    @Provides
    fun provideAuditLogDao(db: ChameleonDatabase): AuditLogDao = db.auditLogDao()

    @Provides
    fun provideIfrTierCacheDao(db: ChameleonDatabase): IfrTierCacheDao = db.ifrTierCacheDao()

    @Provides
    @Singleton
    fun provideIfrTierRepository(
        dao: IfrTierCacheDao,
        keystoreManager: KeystoreManager
    ): IfrTierRepository = IfrTierRepositoryImpl(dao, keystoreManager)

    @Provides
    @Singleton
    fun provideTierGate(repo: IfrTierRepository): TierGate = TierGateImpl(repo)

    @Provides
    @Singleton
    fun provideSecureRuleRepository(dao: SecureRuleDao): SecureRuleRepository {
        return object : SecureRuleRepository {
            override fun observeEnabled(): Flow<List<SecureRule>> = flowOf(emptyList())
            override fun observeAll(): Flow<List<SecureRule>> = flowOf(emptyList())
            override suspend fun getById(id: String): SecureRule? = null
            override suspend fun save(rule: SecureRule) {}
            override suspend fun delete(rule: SecureRule) {}
            override suspend fun deleteAll() {}
            override suspend fun recordTrigger(ruleId: String, timestamp: Long) {}
        }
    }
}

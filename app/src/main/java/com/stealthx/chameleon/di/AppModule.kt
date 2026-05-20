/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.chameleon.di

import android.content.Context
import android.util.Base64
import com.stealthx.data.ChameleonDatabase
import com.stealthx.data.dao.AuditLogDao
import com.stealthx.data.dao.ContactKeyDao
import com.stealthx.data.dao.CryptoKeyDao
import com.stealthx.data.dao.IfrTierCacheDao
import com.stealthx.data.dao.SecureRuleDao
import com.stealthx.data.repository.IfrTierRepositoryImpl
import com.stealthx.data.repository.SecureRuleRepositoryImpl
import com.stealthx.domain.repository.IfrTierRepository
import com.stealthx.domain.repository.SecureRuleRepository
import com.stealthx.domain.rules.RuleEngine
import com.stealthx.domain.tier.TierGate
import com.stealthx.domain.tier.TierGateImpl
import com.stealthx.security.KeystoreManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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
        val prefs = context.getSharedPreferences("chameleon_secure", Context.MODE_PRIVATE)
        val prefKey = "db_passphrase_enc"
        val passphrase: ByteArray
        val stored = prefs.getString(prefKey, null)
        if (stored == null) {
            // First launch: generate a random passphrase, wrap it with a Keystore AES-GCM key
            val raw = ByteArray(32).also { java.security.SecureRandom().nextBytes(it) }
            val blob = keystoreManager.encryptBytes("chameleon_db_key_wrap", raw)
            prefs.edit().putString(prefKey, Base64.encodeToString(blob, Base64.NO_WRAP)).apply()
            passphrase = raw
        } else {
            // Subsequent launches: unwrap from Keystore
            val blob = Base64.decode(stored, Base64.NO_WRAP)
            passphrase = keystoreManager.decryptBytes("chameleon_db_key_wrap", blob)
        }
        return ChameleonDatabase.build(context, passphrase)
    }

    @Provides
    fun provideSecureRuleDao(db: ChameleonDatabase): SecureRuleDao = db.secureRuleDao()

    @Provides
    fun provideCryptoKeyDao(db: ChameleonDatabase): CryptoKeyDao = db.cryptoKeyDao()

    @Provides
    fun provideContactKeyDao(db: ChameleonDatabase): ContactKeyDao = db.contactKeyDao()

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
    fun provideSecureRuleRepository(dao: SecureRuleDao): SecureRuleRepository =
        SecureRuleRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideRuleEngine(): RuleEngine = RuleEngine()
}

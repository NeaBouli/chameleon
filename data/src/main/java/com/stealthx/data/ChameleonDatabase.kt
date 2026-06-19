/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.stealthx.data.dao.AuditLogDao
import com.stealthx.data.dao.ChatSessionDao
import com.stealthx.data.dao.ContactKeyDao
import com.stealthx.data.dao.CryptoKeyDao
import com.stealthx.data.dao.AccessTierCacheDao
import com.stealthx.data.dao.MessageDao
import com.stealthx.data.dao.SecureRuleDao
import com.stealthx.data.entity.AuditLogEntity
import com.stealthx.data.entity.ChatSessionEntity
import com.stealthx.data.entity.ContactKeyEntity
import com.stealthx.data.entity.CryptoKeyEntity
import com.stealthx.data.entity.AccessTierCacheEntity
import com.stealthx.data.entity.MessageEntity
import com.stealthx.data.entity.SecureRuleEntity
import net.sqlcipher.database.SupportFactory

/**
 * Chameleon Room Database — encrypted with SQLCipher.
 *
 * SECURITY:
 * - Database key from Android Keystore via KeystoreManager.getOrCreateAesKey()
 * - Key NEVER stored in SharedPreferences or plaintext
 * - exportSchema = true for migration support
 * - All tables encrypted at rest
 */
@Database(
    entities = [
        SecureRuleEntity::class,
        CryptoKeyEntity::class,
        ContactKeyEntity::class,
        AuditLogEntity::class,
        AccessTierCacheEntity::class,
        MessageEntity::class,
        ChatSessionEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(ChameleonTypeConverters::class)
abstract class ChameleonDatabase : RoomDatabase() {

    abstract fun secureRuleDao(): SecureRuleDao
    abstract fun cryptoKeyDao(): CryptoKeyDao
    abstract fun contactKeyDao(): ContactKeyDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun accessTierCacheDao(): AccessTierCacheDao
    abstract fun messageDao(): MessageDao
    abstract fun chatSessionDao(): ChatSessionDao

    companion object {
        private const val DB_NAME = "chameleon_secure.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS messages (
                        id TEXT NOT NULL PRIMARY KEY,
                        contact_id TEXT NOT NULL,
                        direction TEXT NOT NULL,
                        ciphertext BLOB NOT NULL,
                        nonce BLOB NOT NULL,
                        aad BLOB NOT NULL,
                        padded_length INTEGER NOT NULL,
                        algorithm TEXT NOT NULL,
                        payload_version INTEGER NOT NULL,
                        sent_at INTEGER NOT NULL,
                        delivery_status TEXT NOT NULL,
                        transport_type TEXT NOT NULL,
                        ratchet_dh_public BLOB,
                        ratchet_counter INTEGER,
                        ratchet_prev_counter INTEGER,
                        ratchet_ciphertext BLOB,
                        ratchet_nonce BLOB,
                        ratchet_aad BLOB,
                        ratchet_padded_length INTEGER,
                        ratchet_algorithm TEXT,
                        ratchet_payload_version INTEGER,
                        FOREIGN KEY(contact_id) REFERENCES contact_keys(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_messages_contact_id_sent_at ON messages(contact_id, sent_at)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_messages_delivery_status ON messages(delivery_status)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS chat_sessions (
                        contact_id TEXT NOT NULL PRIMARY KEY,
                        root_key BLOB NOT NULL,
                        send_chain_key BLOB NOT NULL,
                        send_dh_public BLOB NOT NULL,
                        send_dh_private BLOB NOT NULL,
                        send_counter INTEGER NOT NULL,
                        receive_root_key BLOB,
                        receive_chain_key BLOB,
                        receive_dh_public BLOB,
                        receive_counter INTEGER NOT NULL DEFAULT 0,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        FOREIGN KEY(contact_id) REFERENCES contact_keys(id) ON DELETE CASCADE
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS access_tier_cache (
                        source_id TEXT NOT NULL PRIMARY KEY,
                        access_weight INTEGER NOT NULL,
                        tier TEXT NOT NULL,
                        verified_at INTEGER NOT NULL,
                        expires_at INTEGER NOT NULL,
                        hmac BLOB NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun build(context: Context, passphrase: ByteArray): ChameleonDatabase {
            val factory = SupportFactory(passphrase)
            return Room.databaseBuilder(
                context.applicationContext,
                ChameleonDatabase::class.java,
                DB_NAME
            )
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
        }
    }
}

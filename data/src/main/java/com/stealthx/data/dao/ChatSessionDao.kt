/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stealthx.data.entity.ChatSessionEntity

@Dao
interface ChatSessionDao {
    @Query("SELECT * FROM chat_sessions WHERE contact_id = :contactId")
    suspend fun get(contactId: String): ChatSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: ChatSessionEntity)

    @Query("DELETE FROM chat_sessions WHERE contact_id = :contactId")
    suspend fun delete(contactId: String)

    @Query("DELETE FROM chat_sessions")
    suspend fun deleteAll()
}

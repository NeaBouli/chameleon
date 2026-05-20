/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stealthx.data.entity.ContactKeyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactKeyDao {

    @Query("SELECT * FROM contact_keys ORDER BY created_at DESC")
    fun observeAll(): Flow<List<ContactKeyEntity>>

    @Query("SELECT * FROM contact_keys ORDER BY created_at DESC")
    suspend fun getAll(): List<ContactKeyEntity>

    @Query("SELECT * FROM contact_keys WHERE id = :id")
    suspend fun getById(id: String): ContactKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(contact: ContactKeyEntity)

    @Delete
    suspend fun delete(contact: ContactKeyEntity)

    @Query("DELETE FROM contact_keys WHERE id = :id")
    suspend fun deleteById(id: String)
}

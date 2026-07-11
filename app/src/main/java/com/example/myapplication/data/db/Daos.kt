// Author: PKQ32
// Created: 2026-07-11
// Description: Room DAO：小应用与运行日志的增删查

package com.example.myapplication.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MiniAppDao {
    @Query("SELECT * FROM mini_apps ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<MiniAppEntity>>

    @Query("SELECT * FROM mini_apps ORDER BY updatedAt DESC")
    suspend fun getAll(): List<MiniAppEntity>

    @Query("SELECT * FROM mini_apps WHERE id = :id")
    suspend fun getById(id: String): MiniAppEntity?

    @Query("SELECT * FROM mini_apps WHERE runOnHostOpen = 1 AND enabled = 1 LIMIT 1")
    suspend fun getHostOpenApp(): MiniAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MiniAppEntity)

    @Query("DELETE FROM mini_apps WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface RunLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RunLogEntity)

    @Query("SELECT * FROM run_logs WHERE miniAppId = :miniAppId ORDER BY startedAt DESC LIMIT :limit")
    fun observeForApp(miniAppId: String, limit: Int = 20): Flow<List<RunLogEntity>>
}

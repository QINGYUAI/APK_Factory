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

    /** 保证同一时间只有一个小应用占用「打开工厂自动运行」。 */
    @Query("UPDATE mini_apps SET runOnHostOpen = 0, updatedAt = :now WHERE runOnHostOpen = 1 AND id != :exceptId")
    suspend fun clearHostOpenFlags(exceptId: String, now: Long)

    @Query("DELETE FROM mini_apps WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface RunLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RunLogEntity)

    @Query("SELECT * FROM run_logs WHERE miniAppId = :miniAppId ORDER BY startedAt DESC LIMIT :limit")
    fun observeForApp(miniAppId: String, limit: Int = 20): Flow<List<RunLogEntity>>

    /** 仅保留每个小应用最近 [keep] 条，避免日志无限增长。 */
    @Query(
        """
        DELETE FROM run_logs WHERE miniAppId = :miniAppId AND id NOT IN (
            SELECT id FROM run_logs WHERE miniAppId = :miniAppId
            ORDER BY startedAt DESC LIMIT :keep
        )
        """,
    )
    suspend fun prune(miniAppId: String, keep: Int)

    @Query("DELETE FROM run_logs WHERE miniAppId = :miniAppId")
    suspend fun deleteForApp(miniAppId: String)
}

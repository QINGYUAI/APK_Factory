// Author: PKQ32
// Created: 2026-07-11
// Description: 小应用仓库，负责 Entity 与 Domain 映射及 CRUD

package com.example.myapplication.data

import com.example.myapplication.data.db.MiniAppDao
import com.example.myapplication.data.db.MiniAppEntity
import com.example.myapplication.domain.model.MiniApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MiniAppRepository(private val dao: MiniAppDao) {
    fun observeAll(): Flow<List<MiniApp>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun get(id: String): MiniApp? = dao.getById(id)?.toDomain()

    suspend fun save(app: MiniApp) {
        dao.upsert(app.toEntity())
    }

    suspend fun delete(id: String) {
        dao.delete(id)
    }

    suspend fun getHostOpenApp(): MiniApp? = dao.getHostOpenApp()?.toDomain()

    private fun MiniAppEntity.toDomain(): MiniApp =
        MiniApp(
            id = id,
            name = name,
            iconKey = iconKey,
            enabled = enabled,
            runOnHostOpen = runOnHostOpen,
            createdAt = createdAt,
            updatedAt = updatedAt,
            steps = JsonMappers.stepsFromJson(stepsJson),
            triggers = JsonMappers.triggersFromJson(triggersJson),
        )

    private fun MiniApp.toEntity(): MiniAppEntity =
        MiniAppEntity(
            id = id,
            name = name,
            iconKey = iconKey,
            enabled = enabled,
            runOnHostOpen = runOnHostOpen,
            createdAt = createdAt,
            updatedAt = updatedAt,
            stepsJson = JsonMappers.stepsToJson(steps),
            triggersJson = JsonMappers.triggersToJson(triggers),
        )
}

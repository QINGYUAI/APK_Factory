// Author: PKQ32
// Created: 2026-07-11
// Description: 运行日志仓库，支持追加与按应用观察

package com.example.myapplication.data

import com.example.myapplication.data.db.RunLogDao
import com.example.myapplication.data.db.RunLogEntity
import com.example.myapplication.domain.model.RunLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RunLogRepository(private val dao: RunLogDao) {
    suspend fun append(log: RunLog) {
        dao.insert(log.toEntity())
    }

    fun observeForApp(miniAppId: String, limit: Int = 20): Flow<List<RunLog>> =
        dao.observeForApp(miniAppId, limit).map { list -> list.map { it.toDomain() } }

    private fun RunLogEntity.toDomain(): RunLog =
        RunLog(
            id = id,
            miniAppId = miniAppId,
            startedAt = startedAt,
            endedAt = endedAt,
            stepResultsJson = stepResultsJson,
            success = success,
        )

    private fun RunLog.toEntity(): RunLogEntity =
        RunLogEntity(
            id = id,
            miniAppId = miniAppId,
            startedAt = startedAt,
            endedAt = endedAt,
            stepResultsJson = stepResultsJson,
            success = success,
        )
}

// Author: PKQ32
// Created: 2026-07-11
// Description: Room 实体：小应用表与运行日志表

package com.example.myapplication.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "mini_apps")
data class MiniAppEntity(
    @PrimaryKey val id: String,
    val name: String,
    val iconKey: String,
    val enabled: Boolean,
    val runOnHostOpen: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val stepsJson: String,
    val triggersJson: String,
)

@Entity(
    tableName = "run_logs",
    // 历史页按 miniAppId + startedAt 倒序查询，避免日志增长后全表扫描
    indices = [Index(value = ["miniAppId", "startedAt"], name = "index_run_logs_miniAppId_startedAt")],
)
data class RunLogEntity(
    @PrimaryKey val id: String,
    val miniAppId: String,
    val startedAt: Long,
    val endedAt: Long?,
    val stepResultsJson: String,
    val success: Boolean,
)

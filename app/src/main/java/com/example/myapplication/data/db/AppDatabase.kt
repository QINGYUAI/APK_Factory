// Author: PKQ32
// Created: 2026-07-11
// Description: Room 数据库定义，暴露 MiniAppDao 与 RunLogDao

package com.example.myapplication.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * 版本升级约定：
 * 1. 每次改表结构必须 bump version，并在 [com.example.myapplication.di.ServiceLocator] 注册 Migration；
 * 2. schemas/ 下的 JSON 由 Room 编译期导出，用作 Migration / AutoMigration 的依据，需随代码提交。
 *
 * v2：run_logs 增加 (miniAppId, startedAt) 索引，加速运行历史查询。
 */
@Database(
    entities = [MiniAppEntity::class, RunLogEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun miniAppDao(): MiniAppDao
    abstract fun runLogDao(): RunLogDao
}

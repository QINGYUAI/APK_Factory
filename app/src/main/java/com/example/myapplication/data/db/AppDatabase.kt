// Author: PKQ32
// Created: 2026-07-11
// Description: Room 数据库定义，暴露 MiniAppDao 与 RunLogDao

package com.example.myapplication.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [MiniAppEntity::class, RunLogEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun miniAppDao(): MiniAppDao
    abstract fun runLogDao(): RunLogDao
}

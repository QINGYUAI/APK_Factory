// Author: PKQ32
// Created: 2026-07-11
// Description: 基于时间窗口的防抖器，防止同一 key 短时间内重复触发

package com.example.myapplication.engine

class Debouncer(private val windowMs: Long = 3000) {
    private val last = mutableMapOf<String, Long>()

    fun shouldRun(key: String, now: Long = System.currentTimeMillis()): Boolean {
        val prev = last[key]
        if (prev != null && now - prev < windowMs) return false
        last[key] = now
        return true
    }
}

// Author: PKQ32
// Created: 2026-07-11
// Description: 基于时间窗口的防抖器，防止同一 key 短时间内重复触发；过期键会被回收

package com.example.myapplication.engine

class Debouncer(private val windowMs: Long = 3000) {
    private val last = mutableMapOf<String, Long>()

    fun shouldRun(key: String, now: Long = System.currentTimeMillis()): Boolean {
        if (last.size > MAX_ENTRIES) {
            prune(now)
        }
        val prev = last[key]
        if (prev != null && now - prev < windowMs) return false
        last[key] = now
        return true
    }

    /** 清空全部记录（测试或切换账号/配置时使用）。 */
    fun clear() {
        last.clear()
    }

    private fun prune(now: Long) {
        // 保留至少两个防抖窗口，避免把仍在生效的键删掉
        val cutoff = now - maxOf(windowMs * 2, MIN_RETAIN_MS)
        val iterator = last.entries.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().value < cutoff) {
                iterator.remove()
            }
        }
    }

    companion object {
        private const val MAX_ENTRIES = 256
        private const val MIN_RETAIN_MS = 60_000L
    }
}

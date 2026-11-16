// Author: PKQ32
// Created: 2026-07-11
// Description: 防抖窗口、过期键回收与清空单测

package com.example.myapplication.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DebouncerTest {
    @Test
    fun blocksRapidRepeat() {
        val d = Debouncer(windowMs = 3000)
        assertTrue(d.shouldRun("app1", now = 1000))
        assertFalse(d.shouldRun("app1", now = 2000))
        assertTrue(d.shouldRun("app1", now = 4500))
        assertTrue(d.shouldRun("app2", now = 4600))
    }

    @Test
    fun expiresOldEntriesWhenMapGrows() {
        val d = Debouncer(windowMs = 3000)
        val base = 1_000_000L
        repeat(300) { i -> d.shouldRun("key-$i", now = base + i) }
        // 触发清理：所有旧记录都超过保留窗口
        assertTrue(d.shouldRun("fresh", now = base + 100_000))
        assertTrue(d.shouldRun("key-0", now = base + 100_001))
    }

    @Test
    fun clearAllowsImmediateRerun() {
        val d = Debouncer(windowMs = 3000)
        assertTrue(d.shouldRun("app", now = 1000))
        assertFalse(d.shouldRun("app", now = 1500))
        d.clear()
        assertTrue(d.shouldRun("app", now = 1600))
    }
}

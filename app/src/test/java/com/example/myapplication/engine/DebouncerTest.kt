// Author: PKQ32
// Created: 2026-07-11
// Description: Debouncer 防抖逻辑单测

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
}

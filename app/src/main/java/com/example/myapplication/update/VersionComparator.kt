// Author: PKQ32
// Created: 2026-11-16
// Description: 版本号比较：忽略 v 前缀与预发布后缀，按数字段比较

package com.example.myapplication.update

object VersionComparator {

    private val LEADING_VERSION = Regex("""^[vV]?(\d+)(?:\.(\d+))?(?:\.(\d+))?""")

    /** 解析 1、1.2、v1.2.3；无法解析返回 null。 */
    fun parse(raw: String): List<Int>? {
        val match = LEADING_VERSION.find(raw.trim()) ?: return null
        val groups = match.groupValues.drop(1)
        if (groups.firstOrNull().isNullOrEmpty()) return null
        return groups.mapNotNull { it.toIntOrNull() }
    }

    /** [candidate] 是否比 [current] 更新；任一方无法解析时返回 false。 */
    fun isNewer(candidate: String, current: String): Boolean {
        val a = parse(candidate) ?: return false
        val b = parse(current) ?: return false
        val size = maxOf(a.size, b.size)
        for (i in 0 until size) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}

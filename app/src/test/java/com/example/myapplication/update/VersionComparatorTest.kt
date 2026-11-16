// Author: PKQ32
// Created: 2026-11-16
// Description: 版本比较单测：主/次/修订号、v 前缀与预发布后缀

package com.example.myapplication.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionComparatorTest {

    @Test
    fun patchVersionIsNewer() {
        assertTrue(VersionComparator.isNewer("1.0.1", "1.0.0"))
    }

    @Test
    fun minorAndMajorVersionsAreNewer() {
        assertTrue(VersionComparator.isNewer("1.1.0", "1.0.9"))
        assertTrue(VersionComparator.isNewer("2.0", "1.9.9"))
    }

    @Test
    fun vPrefixAndMissingSegmentsAreEquivalent() {
        assertFalse(VersionComparator.isNewer("v1.0.0", "1.0"))
        assertFalse(VersionComparator.isNewer("1.0", "v1.0.0"))
    }

    @Test
    fun olderVersionIsNotNewer() {
        assertFalse(VersionComparator.isNewer("1.0.9", "1.1.0"))
        assertFalse(VersionComparator.isNewer("0.9.9", "1.0.0"))
    }

    @Test
    fun prereleaseSuffixIsIgnoredForNumericComparison() {
        assertTrue(VersionComparator.isNewer("v1.2.0-beta2", "1.1.9"))
        assertFalse(VersionComparator.isNewer("1.2.0-rc1", "1.2.0"))
    }

    @Test
    fun unparsableVersionsNeverCountAsNewer() {
        assertFalse(VersionComparator.isNewer("latest", "1.0.0"))
        assertFalse(VersionComparator.isNewer("1.1.0", "dev"))
        assertNull(VersionComparator.parse("v"))
    }

    @Test
    fun parseExtractsLeadingNumericSegments() {
        assertEquals(listOf(1, 2, 3), VersionComparator.parse("v1.2.3"))
        assertEquals(listOf(2, 0), VersionComparator.parse("2.0-beta"))
    }
}

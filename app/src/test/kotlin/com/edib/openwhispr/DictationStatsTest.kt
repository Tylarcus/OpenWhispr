package com.edib.openwhispr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DictationStatsTest {
    @Test fun emptyText() = assertEquals(0, DictationStats.countWords(" … "))
    @Test fun punctuationAndContractions() =
        assertEquals(6, DictationStats.countWords("Hello, world! I'm testing voice-input."))
    @Test fun multilineAndUnicode() =
        assertEquals(4, DictationStats.countWords("Good morning\n¡Hola! café"))
    @Test fun weightedAverageWpm() {
        val s = DictationStats.Snapshot(300, 100, 3, 120000L, 30, 120)
        assertEquals(150, s.averageWpm)
    }
    @Test fun mondayBasedWeeks() {
        assertEquals(LocalDate.of(2026, 10, 26),
            DictationStats.startOfWeek(LocalDate.of(2026, 11, 1)))
        assertEquals(LocalDate.of(2026, 11, 2),
            DictationStats.startOfWeek(LocalDate.of(2026, 11, 2)))
    }
    @Test fun scopeSelection() {
        val s = DictationStats.Snapshot(500, 100, 5, 180000, 40, 120,
            60_000, 1, 240, 120_000, 3, 20_000)
        assertEquals(100L, s.forScope(DictationStats.Scope.TODAY).words)
        assertEquals(240L, s.forScope(DictationStats.Scope.WEEK).words)
        assertEquals(500L, s.forScope(DictationStats.Scope.LIFETIME).words)
        assertEquals(40L, s.forScope(DictationStats.Scope.LAST).words)
    }
    @Test fun comparisonsNeverExaggerateThreshold() {
        assertNull(WordComparisons.forCount(9))
        assertEquals(10L, WordComparisons.forCount(10)?.words)
        assertEquals(769L, WordComparisons.forCount(999)?.words)
        assertTrue(WordComparisons.forCount(100_000)?.description?.contains("400") == true)
        assertEquals(33, WordComparisons.milestones.size)
    }
}

package com.edib.openwhispr

import org.junit.Assert.assertEquals
import org.junit.Test

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
}

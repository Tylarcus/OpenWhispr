package com.edib.openwhispr

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream

class AudioSilenceReducerTest {
    private fun frame(magnitude: Int, milliseconds: Int): ByteArray {
        val out = ByteArray(milliseconds * 16_000 / 1000 * 2)
        // A 20ms continuous synthetic tone-like source at specified amplitude.
        for (i in out.indices step 2) {
            val value = if ((i / 2) % 32 < 16) magnitude else -magnitude
            out[i] = value.toByte()
            out[i + 1] = (value shr 8).toByte()
        }
        return out
    }

    private fun concat(vararg segments: ByteArray) = ByteArrayOutputStream().apply {
        for (segment in segments) write(segment)
    }.toByteArray()

    @Test fun pausesUnderTwoSecondsRemainUntouched() {
        val input = concat(frame(2400, 500), frame(0, 1200), frame(1900, 500))
        val result = AudioSilenceReducer.reduce(input)
        assertTrue(result.hasActivity)
        assertArrayEquals(input, result.pcm)
        assertEquals(0, result.removedMilliseconds)
    }

    @Test fun longQuietPauseBetweenTwoUtterancesIsShortened() {
        val first = frame(2400, 600)
        val last = frame(1700, 500)
        val input = concat(first, frame(0, 20000), last)
        val result = AudioSilenceReducer.reduce(input)
        assertTrue(result.hasActivity)
        assertTrue(result.removedMilliseconds > 18000)
        assertArrayEquals(first, result.pcm.copyOfRange(0, first.size))
        assertArrayEquals(last, result.pcm.copyOfRange(result.pcm.size - last.size, result.pcm.size))
    }

    @Test fun longQuietTailDoesNotReachWhisper() {
        val input = concat(frame(2500, 400), frame(0, 20000))
        val result = AudioSilenceReducer.reduce(input)
        assertTrue(result.removedMilliseconds >= 19000)
        assertTrue(result.pcm.size < frame(2500, 1000).size)
    }

    @Test fun entirelySilentRecordingIsSkipped() {
        val result = AudioSilenceReducer.reduce(frame(0, 25000))
        assertFalse(result.hasActivity)
        assertEquals(0, result.pcm.size)
    }

    @Test fun backgroundHissIsSkippedIfReliablyQuiet() {
        val result = AudioSilenceReducer.reduce(frame(35, 16000))
        assertFalse(result.hasActivity)
        assertTrue(result.pcm.isEmpty())
    }

    @Test fun isolatedClickInTwentySecondsOfSilenceDoesNotProduceDictation() {
        val input = concat(frame(0, 9000), frame(5000, 20), frame(0, 11000))
        val result = AudioSilenceReducer.reduce(input)
        assertFalse(result.hasActivity)
        assertTrue(result.pcm.isEmpty())
    }

    @Test fun shortQuickUtteranceIsKept() {
        val input = frame(1200, 160)
        val result = AudioSilenceReducer.reduce(input)
        assertTrue(result.hasActivity)
        assertArrayEquals(input, result.pcm)
    }
}

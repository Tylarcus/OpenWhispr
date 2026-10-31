package com.edib.openwhispr

import java.io.ByteArrayOutputStream
import kotlin.math.abs

/**
 * Conservative silence cleanup for 16 kHz, 16-bit mono microphone PCM.
 *
 * Whisper can hallucinate familiar phrases when a short utterance is followed
 * by tens of seconds of near-silence. Preserve genuine short pauses and spoken
 * audio, but shorten *long, reliably quiet* runs before sending audio to ASR.
 *
 * This is not a VAD or a promise to catch all background noise. When in doubt,
 * treat a frame as active and preserve it rather than cut potentially spoken
 * words. No cloud requests or changes to the cleanup prompt happen here.
 */
object AudioSilenceReducer {
    const val SAMPLE_RATE = 16_000
    private const val FRAME_MS = 20
    private const val FRAME_BYTES = SAMPLE_RATE * FRAME_MS / 1000 * 2
    private const val MIN_LONG_PAUSE_FRAMES = 110 // 2.2 seconds
    private const val CONTEXT_FRAMES = 18 // 360 ms either side
    private const val MIN_VOICE_PEAK = 550
    private const val MIN_VOICE_MEAN_ABS = 145

    data class Result(val pcm: ByteArray, val hasActivity: Boolean, val removedMilliseconds: Int)

    fun reduce(pcm: ByteArray): Result {
        if (pcm.isEmpty()) return Result(pcm, false, 0)
        val frames = (pcm.size + FRAME_BYTES - 1) / FRAME_BYTES
        val active = BooleanArray(frames)
        for (f in 0 until frames) {
            val begin = f * FRAME_BYTES
            val end = minOf(begin + FRAME_BYTES, pcm.size)
            var peak = 0
            var sum = 0L
            var count = 0
            var pos = begin
            while (pos + 1 < end) {
                val lo = pcm[pos].toInt() and 0xff
                val hi = pcm[pos + 1].toInt()
                val v = ((hi shl 8) or lo).toShort().toInt()
                val absSample = abs(v)
                peak = maxOf(peak, absSample)
                sum += absSample
                count++
                pos += 2
            }
            val mean = if (count > 0) sum.toFloat() / count else 0f
            active[f] = peak >= MIN_VOICE_PEAK || mean >= MIN_VOICE_MEAN_ABS
        }
        // A few isolated loud frames are commonly handling noise or clicks,
        // not speech. Do not send a near-silent recording to Whisper based on
        // one 20ms spike (the old global-peak test allowed exactly that).
        // Compare frame energy with the quieter background frames. Whisper
        // frequently hallucinates if a microphone records constant room noise
        // with no voice. An isolated tap/click or even constant fan noise must
        // not be treated as intelligible speech.
        val frameMeanAbs = FloatArray(frames)
        for (f in 0 until frames) {
            val begin = f * FRAME_BYTES
            val end = minOf(begin + FRAME_BYTES, pcm.size)
            var sum = 0L
            var count = 0
            var pos = begin
            while (pos + 1 < end) {
                val value = ((pcm[pos + 1].toInt() shl 8) or
                    (pcm[pos].toInt() and 0xFF)).toShort().toInt()
                sum += abs(value)
                count++
                pos += 2
            }
            frameMeanAbs[f] = if (count > 0) sum.toFloat() / count else 0f
        }
        val sortedEnergy = frameMeanAbs.sorted()
        val noiseFloor = sortedEnergy[(sortedEnergy.size / 5).coerceAtMost(sortedEnergy.lastIndex)]
        val speechThreshold = maxOf(210f, noiseFloor * 2.5f)
        val loudFrameCount = frameMeanAbs.count { it >= speechThreshold }
        val denseWindow = frameMeanAbs.indices.any { start ->
            // At least 160ms of high-energy frames in an 800ms window.
            var count = 0
            for (j in start until minOf(start + 40, frames)) {
                if (frameMeanAbs[j] >= speechThreshold) count++
            }
            count >= 8
        }
        val shortSpeechOnly = frames <= 60 &&
            frameMeanAbs.average() >= 430.0 && active.count { it } >= 8
        if (!(denseWindow && loudFrameCount >= 8) && !shortSpeechOnly) {
            return Result(ByteArray(0), false, pcm.size * 1000 / (SAMPLE_RATE * 2))
        }

        if (active.count { it } < 4) {
            return Result(ByteArray(0), false, pcm.size * 1000 / (SAMPLE_RATE * 2))
        }

        val retain = BooleanArray(frames) { true }
        var runStart = 0
        while (runStart < frames) {
            if (active[runStart]) {
                runStart++
                continue
            }
            var runEnd = runStart + 1
            while (runEnd < frames && !active[runEnd]) runEnd++
            if (runEnd - runStart >= MIN_LONG_PAUSE_FRAMES) {
                val before = if (runStart == 0) 0 else CONTEXT_FRAMES
                val after = if (runEnd == frames) 0 else CONTEXT_FRAMES
                val removeFrom = runStart + before
                val removeTo = runEnd - after
                for (i in removeFrom until maxOf(removeFrom, removeTo)) retain[i] = false
            }
            runStart = runEnd
        }

        if (retain.all { it }) return Result(pcm, true, 0)
        val output = ByteArrayOutputStream(pcm.size)
        for (f in 0 until frames) {
            if (retain[f]) {
                val begin = f * FRAME_BYTES
                output.write(pcm, begin, minOf(FRAME_BYTES, pcm.size - begin))
            }
        }
        val shortened = output.toByteArray()
        return Result(shortened, true, (pcm.size - shortened.size) * 1000 / (SAMPLE_RATE * 2))
    }
}

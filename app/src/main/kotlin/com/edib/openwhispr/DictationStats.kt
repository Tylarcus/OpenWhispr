package com.edib.openwhispr

import android.content.Context
import org.json.JSONObject
import java.time.LocalDate

/**
 * Private, on-device dictation statistics. No audio or transcription text is stored.
 * Average WPM uses total recognized words divided by captured audio time.
 * This is not speaking-only WPM: intentional pauses are included.
 */
object DictationStats {
    private const val FILE = "dictation_statistics"
    private const val DAY_BUCKETS = "daily_buckets"
    private val wordRegex = Regex("""[\p{L}\p{N}]+(?:['’][\p{L}\p{N}]+)*""")

    enum class Scope { TODAY, WEEK, LIFETIME, LAST }

    data class Period(val words: Long, val recordingMs: Long, val dictations: Long) {
        val wpm: Int get() = if (recordingMs <= 0L) 0
            else (words * 60_000.0 / recordingMs).toInt()
    }

    data class Snapshot(
        val totalWords: Long, val todayWords: Long, val dictations: Long,
        val recordingMs: Long, val lastWords: Int, val lastWpm: Int,
        val todayRecordingMs: Long = 0, val todayDictations: Long = 0,
        val weekWords: Long = 0, val weekRecordingMs: Long = 0,
        val weekDictations: Long = 0, val lastRecordingMs: Long = 0
    ) {
        val averageWpm: Int get() = if (recordingMs <= 0L) 0
            else (totalWords * 60_000.0 / recordingMs).toInt()

        fun forScope(scope: Scope): Period = when (scope) {
            Scope.TODAY -> Period(todayWords, todayRecordingMs, todayDictations)
            Scope.WEEK -> Period(weekWords, weekRecordingMs, weekDictations)
            Scope.LIFETIME -> Period(totalWords, recordingMs, dictations)
            Scope.LAST -> Period(lastWords.toLong(), lastRecordingMs, if (lastWords > 0) 1 else 0)
        }
    }

    fun countWords(text: String): Int = wordRegex.findAll(text).count()

    fun startOfWeek(today: LocalDate): LocalDate =
        today.minusDays((today.dayOfWeek.value - 1).toLong()) // Monday–Sunday

    @Synchronized
    fun record(context: Context, text: String, durationMs: Long) {
        val count = countWords(text)
        if (count == 0 || durationMs <= 0L) return
        val p = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val date = LocalDate.now()
        val today = date.toString()
        val oldToday = if (p.getString("day", "") == today) p.getLong("today_words", 0) else 0L
        val oldMs = if (p.getString("day", "") == today) p.getLong("today_ms", 0) else 0L
        val oldSessions = if (p.getString("day", "") == today) p.getLong("today_sessions", 0) else 0L

        val buckets = readBuckets(p.getString(DAY_BUCKETS, null))
        // Retain enough recent daily aggregates for weekly periods across week boundaries.
        val cutoff = date.minusDays(15).toString()
        val keys = buckets.keys()
        val expired = mutableListOf<String>()
        while (keys.hasNext()) {
            val key = keys.next()
            if (key < cutoff || key > today) expired.add(key)
        }
        expired.forEach { buckets.remove(it) }

        val day = buckets.optJSONObject(today) ?: JSONObject()
        val dayWords = day.optLong("words", oldToday).coerceAtLeast(oldToday) + count
        val dayMs = day.optLong("ms", oldMs).coerceAtLeast(oldMs) + durationMs
        val daySessions = day.optLong("sessions", oldSessions).coerceAtLeast(oldSessions) + 1L
        buckets.put(today, JSONObject()
            .put("words", dayWords)
            .put("ms", dayMs)
            .put("sessions", daySessions))
        p.edit()
            .putLong("total_words", p.getLong("total_words", 0L) + count)
            .putLong("total_ms", p.getLong("total_ms", 0L) + durationMs)
            .putLong("dictations", p.getLong("dictations", 0L) + 1L)
            .putString("day", today)
            .putLong("today_words", dayWords)
            .putLong("today_ms", dayMs)
            .putLong("today_sessions", daySessions)
            .putString(DAY_BUCKETS, buckets.toString())
            .putInt("last_words", count)
            .putLong("last_ms", durationMs)
            .putInt("last_wpm", (count * 60_000.0 / durationMs).toInt())
            .apply()
    }

    @Synchronized
    fun snapshot(context: Context, date: LocalDate = LocalDate.now()): Snapshot {
        val p = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val today = date.toString()
        val sameDay = p.getString("day", "") == today
        val daily = readBuckets(p.getString(DAY_BUCKETS, null))
        // Live weekly totals come only from THIS calendar week, never a rolling week.
        var weekWords = 0L
        var weekMs = 0L
        var weekSessions = 0L
        val start = startOfWeek(date)
        for (offset in 0 until 7) {
            val d = start.plusDays(offset.toLong())
            if (d.isAfter(date)) break
            val bucket = daily.optJSONObject(d.toString()) ?: continue
            weekWords += bucket.optLong("words", 0L)
            weekMs += bucket.optLong("ms", 0L)
            weekSessions += bucket.optLong("sessions", 0L)
        }
        // Backwards compatibility with first beta's simple daily counter:
        // it had no daily buckets; its total before this day cannot be
        // reconstructed as weekly history, so do not fabricate it.
        val todayWords = if (sameDay) p.getLong("today_words", 0L) else 0L
        val todayMs = if (sameDay) p.getLong("today_ms", 0L) else 0L
        val todaySessions = if (sameDay) p.getLong("today_sessions", 0L) else 0L
        if (daily.optJSONObject(today) == null && sameDay) {
            weekWords += todayWords
            weekMs += todayMs
            weekSessions += todaySessions
        }
        return Snapshot(
            totalWords = p.getLong("total_words", 0L),
            todayWords = todayWords,
            dictations = p.getLong("dictations", 0L),
            recordingMs = p.getLong("total_ms", 0L),
            lastWords = p.getInt("last_words", 0),
            lastWpm = p.getInt("last_wpm", 0),
            todayRecordingMs = todayMs,
            todayDictations = todaySessions,
            weekWords = weekWords,
            weekRecordingMs = weekMs,
            weekDictations = weekSessions,
            lastRecordingMs = p.getLong("last_ms", 0L)
        )
    }

    private fun readBuckets(json: String?): JSONObject =
        try { JSONObject(json ?: "{}") } catch (_: Exception) { JSONObject() }

    @Synchronized
    fun reset(context: Context) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().clear().commit()
    }
}

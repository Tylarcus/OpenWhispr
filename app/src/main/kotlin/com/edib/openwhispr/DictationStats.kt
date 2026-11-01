package com.edib.openwhispr

import android.content.Context
import java.time.LocalDate

/** Local-only counters: no audio or transcript text is stored. */
object DictationStats {
    private const val FILE = "dictation_statistics"
    private val wordRegex = Regex("""[\p{L}\p{N}]+(?:['’][\p{L}\p{N}]+)*""")

    data class Snapshot(
        val totalWords: Long, val todayWords: Long, val dictations: Long,
        val recordingMs: Long, val lastWords: Int, val lastWpm: Int
    ) {
        val averageWpm: Int
            get() = if (recordingMs <= 0L) 0
                else (totalWords * 60_000.0 / recordingMs).toInt()
    }

    fun countWords(text: String): Int = wordRegex.findAll(text).count()

    @Synchronized
    fun record(context: Context, text: String, durationMs: Long) {
        val count = countWords(text)
        if (count == 0 || durationMs <= 0L) return
        val p = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val today = LocalDate.now().toString()
        val oldToday = if (p.getString("day", "") == today) p.getLong("today_words", 0L) else 0L
        p.edit()
            .putLong("total_words", p.getLong("total_words", 0L) + count)
            .putLong("today_words", oldToday + count)
            .putString("day", today)
            .putLong("dictations", p.getLong("dictations", 0L) + 1L)
            .putLong("total_ms", p.getLong("total_ms", 0L) + durationMs)
            .putInt("last_words", count)
            .putInt("last_wpm", (count * 60_000.0 / durationMs).toInt())
            .apply()
    }

    fun snapshot(context: Context): Snapshot {
        val p = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        return Snapshot(
            totalWords = p.getLong("total_words", 0L),
            todayWords = if (p.getString("day", "") == LocalDate.now().toString())
                p.getLong("today_words", 0L) else 0L,
            dictations = p.getLong("dictations", 0L),
            recordingMs = p.getLong("total_ms", 0L),
            lastWords = p.getInt("last_words", 0),
            lastWpm = p.getInt("last_wpm", 0)
        )
    }

    fun reset(context: Context) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().clear().apply()
    }
}

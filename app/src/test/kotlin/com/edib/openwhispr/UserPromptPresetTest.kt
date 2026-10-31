package com.edib.openwhispr

import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Test

class UserPromptPresetTest {
    @Test fun originalHiddenPromptIsNotReplaced() {
        assertTrue(PostProcessor.DEFAULT_PROMPT.startsWith(
            "You are a literal dictation cleanup layer"
        ))
        assertFalse(PostProcessor.DEFAULT_PROMPT.contains("PARAGRAPH STYLE:"))
    }

    @Test fun defaultPresetIsEditableAndOnUnlessSwitchedOff() {
        val prefs = FakePrefs()
        assertTrue(UserPromptPreset.enabled(prefs))
        assertTrue(UserPromptPreset.presetText(prefs).contains("FREQUENT, NATURAL PARAGRAPH BREAKS"))
        assertEquals(UserPromptPreset.DEFAULT_TEXT, UserPromptPreset.combinedInstructions(prefs))
        prefs.edit().putBoolean(UserPromptPreset.ENABLED_KEY, false).apply()
        assertEquals("", UserPromptPreset.combinedInstructions(prefs))
    }

    @Test fun migratingPastedOriginalPreservesTextWithoutDuplicating() {
        val copied = UserPromptPreset.DEFAULT_TEXT
        val prefs = FakePrefs(mapOf(UserPromptPreset.EXTRA_KEY to copied))
        UserPromptPreset.migrateIfNeeded(prefs)
        assertEquals(copied, UserPromptPreset.presetText(prefs))
        assertEquals("", prefs.getString(UserPromptPreset.EXTRA_KEY, null))
        assertEquals(copied, UserPromptPreset.combinedInstructions(prefs))
        UserPromptPreset.migrateIfNeeded(prefs)
        assertEquals(copied, UserPromptPreset.combinedInstructions(prefs))
    }

    @Test fun unrelatedCustomInstructionsSurviveUpgrade() {
        val prefs = FakePrefs(mapOf(UserPromptPreset.EXTRA_KEY to "Always spell my dog's name Lexi."))
        UserPromptPreset.migrateIfNeeded(prefs)
        assertEquals("Always spell my dog's name Lexi.", prefs.getString(UserPromptPreset.EXTRA_KEY, null))
        val combined = UserPromptPreset.combinedInstructions(prefs)
        assertTrue(combined.contains("FREQUENT, NATURAL PARAGRAPH BREAKS"))
        assertTrue(combined.endsWith("Always spell my dog's name Lexi."))
        prefs.edit().putBoolean(UserPromptPreset.ENABLED_KEY, false).apply()
        assertEquals("Always spell my dog's name Lexi.", UserPromptPreset.combinedInstructions(prefs))
    }

    /** Pure in-memory SharedPreferences for JVM unit tests, without device access. */
    private class FakePrefs(initial: Map<String, Any?> = emptyMap()) : SharedPreferences {
        private val data = initial.toMutableMap()
        override fun getAll(): MutableMap<String, *> = data.toMutableMap()
        override fun getString(key: String?, defValue: String?): String? =
            data[key] as? String ?: defValue
        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            (data[key] as? Set<String>)?.toMutableSet() ?: defValues
        override fun getInt(key: String?, defValue: Int) = data[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long) = data[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float) = data[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean) = data[key] as? Boolean ?: defValue
        override fun contains(key: String?) = data.containsKey(key)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun edit(): SharedPreferences.Editor = Editor()

        inner class Editor : SharedPreferences.Editor {
            override fun putString(key: String?, value: String?) = applyValue(key, value)
            override fun putStringSet(key: String?, values: MutableSet<String>?) = applyValue(key, values)
            override fun putInt(key: String?, value: Int) = applyValue(key, value)
            override fun putLong(key: String?, value: Long) = applyValue(key, value)
            override fun putFloat(key: String?, value: Float) = applyValue(key, value)
            override fun putBoolean(key: String?, value: Boolean) = applyValue(key, value)
            override fun remove(key: String?) = apply { if (key != null) data.remove(key) }
            override fun clear() = apply { data.clear() }
            override fun commit(): Boolean = true
            override fun apply() {}
            private fun applyValue(key: String?, value: Any?): SharedPreferences.Editor = apply {
                if (key != null) data[key] = value
            }
        }
    }
}

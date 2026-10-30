package com.edib.openwhispr

import android.text.InputType

/**
 * Classification of the editor that caused the current Gboard layout.
 * This identifies input semantics, not a different installed keyboard app.
 * No user-entered content is inspected.
 */
enum class KeyboardLayoutProfile { TEXT, NUMBER, SECURE }

object KeyboardLayoutClassifier {
    /**
     * HTML can use type="text" inputmode="numeric", which brings up a
     * numeric keypad even though accessibility reports TYPE_CLASS_TEXT.
     * Use only fixed field labels/hints as a best-effort fallback. Do not
     * read the field value or any typed text.
     */
    fun classify(
        inputType: Int,
        isPassword: Boolean,
        fieldLabel: String? = null
    ): KeyboardLayoutProfile {
        if (isPassword) return KeyboardLayoutProfile.SECURE
        val fieldClass = inputType and InputType.TYPE_MASK_CLASS
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        if (fieldClass == InputType.TYPE_CLASS_NUMBER &&
            variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
        ) return KeyboardLayoutProfile.SECURE

        if (fieldClass == InputType.TYPE_CLASS_TEXT &&
            variation in setOf(
                InputType.TYPE_TEXT_VARIATION_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
            )
        ) return KeyboardLayoutProfile.SECURE

        if (fieldClass == InputType.TYPE_CLASS_NUMBER ||
            fieldClass == InputType.TYPE_CLASS_PHONE ||
            fieldClass == InputType.TYPE_CLASS_DATETIME
        ) return KeyboardLayoutProfile.NUMBER

        // Static accessible aria-label / hint of a WebView input field.
        // Restrict to a few unambiguous labels; arbitrary written content
        // never influences classification.
        val label = fieldLabel?.trim()?.lowercase(java.util.Locale.ROOT).orEmpty()
        if (fieldClass == InputType.TYPE_CLASS_TEXT && label.isNotEmpty()) {
            if (Regex("""\\b(pin|passcode|password|verification code|security code)\\b""")
                    .containsMatchIn(label)) return KeyboardLayoutProfile.SECURE
            if (Regex("""\\b(reps|repetitions|numeric|digits|quantity|number of|weight|kilograms|kg|lbs)\\b""")
                    .containsMatchIn(label)) return KeyboardLayoutProfile.NUMBER
        }
        return KeyboardLayoutProfile.TEXT
    }
}

/** For ordinary number fields, let the user decide whether to hide or
 * use normal/four separately remembered positions. Secure fields always hide.
 */
enum class NumericKeyboardBehavior(val key: String) {
    HIDE("hide"),
    SHARED("shared"),
    SEPARATE("separate");

    companion object {
        fun fromPreference(raw: String?): NumericKeyboardBehavior =
            entries.firstOrNull { it.key == raw } ?: HIDE
    }
}

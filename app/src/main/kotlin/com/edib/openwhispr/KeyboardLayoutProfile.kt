package com.edib.openwhispr

import android.text.InputType

/**
 * Classification of the editor that caused the current Gboard layout.
 * This identifies input semantics, not a different installed keyboard app.
 * No user-entered content is inspected.
 */
enum class KeyboardLayoutProfile { TEXT, NUMBER, SECURE }

object KeyboardLayoutClassifier {
    fun classify(inputType: Int, isPassword: Boolean): KeyboardLayoutProfile {
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

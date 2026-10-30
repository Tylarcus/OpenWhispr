package com.edib.openwhispr

/** Full-screen locations remembered independently for each relevant mode. */
enum class MicrophoneMode { KEYBOARD, FLOATING, RECORDING_DOCK, TYPING_DOCK }

/** Action to take when a user edits text without recording. */
enum class TypingAction(val preferenceValue: String) {
    POSITION_3("position3"),
    HIDE("hide"),
    POSITION_4("position4");

    companion object {
        fun fromPreference(value: String?): TypingAction =
            entries.firstOrNull { it.preferenceValue == value } ?: POSITION_3
    }
}

object MicrophoneModeResolver {
    fun resolve(
        keyboardVisible: Boolean,
        recording: Boolean,
        recordingDockEnabled: Boolean,
        calibration: MicrophoneMode? = null,
        suggestionsBusy: Boolean = false,
        suggestionsDockEnabled: Boolean = false,
        typingAction: TypingAction = TypingAction.POSITION_3
    ): MicrophoneMode {
        if (calibration != null) return calibration
        if (!keyboardVisible) return MicrophoneMode.FLOATING
        // Dictation always wins over typing avoidance settings.
        if (recording && recordingDockEnabled) return MicrophoneMode.RECORDING_DOCK
        if (suggestionsBusy && suggestionsDockEnabled && !recording) {
            return when (typingAction) {
                TypingAction.POSITION_3 -> MicrophoneMode.RECORDING_DOCK
                TypingAction.POSITION_4 -> MicrophoneMode.TYPING_DOCK
                TypingAction.HIDE -> MicrophoneMode.KEYBOARD // visibility computed separately
            }
        }
        return MicrophoneMode.KEYBOARD
    }

    fun shouldHideForTyping(
        keyboardVisible: Boolean,
        recording: Boolean,
        suggestionsBusy: Boolean,
        suggestionsDockEnabled: Boolean,
        typingAction: TypingAction
    ): Boolean =
        keyboardVisible && !recording && suggestionsBusy &&
            suggestionsDockEnabled && typingAction == TypingAction.HIDE
}

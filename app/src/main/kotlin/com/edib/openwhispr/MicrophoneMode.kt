package com.edib.openwhispr

/** The three user-configurable positions of the dictation microphone. */
enum class MicrophoneMode { KEYBOARD, FLOATING, RECORDING_DOCK }

/** Pure layout decision: no text monitoring, timers, or keyboard heuristics. */
object MicrophoneModeResolver {
    fun resolve(
        keyboardVisible: Boolean,
        recording: Boolean,
        recordingDockEnabled: Boolean,
        calibration: MicrophoneMode? = null,
        suggestionsBusy: Boolean = false,
        suggestionsDockEnabled: Boolean = false
    ): MicrophoneMode {
        if (calibration != null) return calibration
        if (!keyboardVisible) return MicrophoneMode.FLOATING
        return if ((recording && recordingDockEnabled) ||
            (suggestionsBusy && suggestionsDockEnabled))
            MicrophoneMode.RECORDING_DOCK
        else
            MicrophoneMode.KEYBOARD
    }
}

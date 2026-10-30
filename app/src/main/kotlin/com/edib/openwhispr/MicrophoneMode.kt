package com.edib.openwhispr

/** Keeps layout decisions independent of Android's accessibility events. */
enum class MicrophoneMode { KEYBOARD, FLOATING, TYPING }

object MicrophoneModeResolver {
    fun resolve(
        keyboardVisible: Boolean,
        typingRecently: Boolean,
        typingEnabled: Boolean,
        calibration: MicrophoneMode? = null
    ): MicrophoneMode {
        if (calibration != null) return calibration
        if (!keyboardVisible) return MicrophoneMode.FLOATING
        return if (typingEnabled && typingRecently) MicrophoneMode.TYPING
        else MicrophoneMode.KEYBOARD
    }
}

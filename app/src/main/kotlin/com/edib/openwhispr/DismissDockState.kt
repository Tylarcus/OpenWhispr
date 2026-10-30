package com.edib.openwhispr

/**
 * Hiding the microphone survives keyboard toggles until the next real
 * closed -> open transition. Closing while recording transcribes clipboard-only.
 */
class DismissDockState {
    var dismissed: Boolean = false
        private set
    private var sawKeyboardClosed = false

    fun dismiss(keyboardVisible: Boolean) {
        dismissed = true
        sawKeyboardClosed = !keyboardVisible
    }

    fun onKeyboardChanged(keyboardVisible: Boolean) {
        if (!dismissed) return
        if (!keyboardVisible) sawKeyboardClosed = true
        else if (sawKeyboardClosed) {
            dismissed = false
            sawKeyboardClosed = false
        }
    }
}

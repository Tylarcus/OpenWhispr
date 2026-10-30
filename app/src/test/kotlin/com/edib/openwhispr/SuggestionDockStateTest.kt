package com.edib.openwhispr

import org.junit.Assert.*
import org.junit.Test

class SuggestionDockStateTest {
    @Test fun typingFallbackDoesNotExpireWithTime() {
        val state = SuggestionDockState()
        state.onEdit()
        assertTrue(state.shouldUseAlternativePosition)
        assertTrue(state.shouldUseAlternativePosition)
        state.onKeyboardClosed()
        assertFalse(state.shouldUseAlternativePosition)
    }
    @Test fun recognizedGboardStripOverridesEditFallback() {
        val state = SuggestionDockState()
        state.onEdit()
        state.onRecognizedStructure(true)
        assertTrue(state.shouldUseAlternativePosition)
        state.onRecognizedStructure(false)
        assertFalse(state.shouldUseAlternativePosition)
    }
    @Test fun newEditorResetsStickyFallback() {
        val state = SuggestionDockState()
        state.onEdit()
        state.onEditorChanged()
        assertFalse(state.shouldUseAlternativePosition)
    }
    @Test fun suggestionModeUsesPositionThreeWithoutRecording() {
        assertEquals(MicrophoneMode.RECORDING_DOCK,
            MicrophoneModeResolver.resolve(
                keyboardVisible = true, recording = false, recordingDockEnabled = false,
                suggestionsBusy = true, suggestionsDockEnabled = true
            ))
        assertEquals(MicrophoneMode.KEYBOARD,
            MicrophoneModeResolver.resolve(
                keyboardVisible = true, recording = false, recordingDockEnabled = false,
                suggestionsBusy = true, suggestionsDockEnabled = false
            ))
    }
}

package com.edib.openwhispr

import org.junit.Assert.*
import org.junit.Test

class DismissDockStateTest {
    @Test fun idleDismissWaitsForFreshKeyboardOpen() {
        val state = DismissDockState()
        state.dismiss(keyboardVisible = true)
        assertTrue(state.dismissed)
        state.onKeyboardChanged(true)
        assertTrue(state.dismissed)
        state.onKeyboardChanged(false)
        assertTrue(state.dismissed)
        state.onKeyboardChanged(true)
        assertFalse(state.dismissed)
    }

    @Test fun dismissDuringClosedKeyboardAllowsNextOpen() {
        val state = DismissDockState()
        state.dismiss(keyboardVisible = false)
        assertTrue(state.dismissed)
        state.onKeyboardChanged(true)
        assertFalse(state.dismissed)
    }
}

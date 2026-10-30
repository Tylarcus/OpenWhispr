package com.edib.openwhispr

import org.junit.Assert.assertEquals
import org.junit.Test

class MicrophoneModeTest {
    @Test fun normalKeyboardUsesSavedKeyboardSpot() {
        assertEquals(MicrophoneMode.KEYBOARD,
            MicrophoneModeResolver.resolve(true, false, true))
    }

    @Test fun typingUsesThirdSpotOnlyWhenOptedIn() {
        assertEquals(MicrophoneMode.TYPING,
            MicrophoneModeResolver.resolve(true, true, true))
        assertEquals(MicrophoneMode.KEYBOARD,
            MicrophoneModeResolver.resolve(true, true, false))
    }

    @Test fun closingKeyboardChoosesFloatingEvenAfterTyping() {
        assertEquals(MicrophoneMode.FLOATING,
            MicrophoneModeResolver.resolve(false, true, true))
    }

    @Test fun calibrationOverridesOnlyWhileInWizard() {
        assertEquals(MicrophoneMode.TYPING,
            MicrophoneModeResolver.resolve(true, false, false, MicrophoneMode.TYPING))
        assertEquals(MicrophoneMode.FLOATING,
            MicrophoneModeResolver.resolve(true, false, false, MicrophoneMode.FLOATING))
    }
}

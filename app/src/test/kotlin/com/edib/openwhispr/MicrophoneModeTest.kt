package com.edib.openwhispr

import org.junit.Assert.assertEquals
import org.junit.Test

class MicrophoneModeTest {
    @Test fun keyboardOpenAndIdleAlwaysUsesPositionOne() {
        assertEquals(MicrophoneMode.KEYBOARD,
            MicrophoneModeResolver.resolve(true, false, true))
    }

    @Test fun recordingWithKeyboardOpenUsesPositionThreeWhenOptedIn() {
        assertEquals(MicrophoneMode.RECORDING_DOCK,
            MicrophoneModeResolver.resolve(true, true, true))
        assertEquals(MicrophoneMode.KEYBOARD,
            MicrophoneModeResolver.resolve(true, true, false))
    }

    @Test fun recordingWithKeyboardClosedUsesPositionTwo() {
        assertEquals(MicrophoneMode.FLOATING,
            MicrophoneModeResolver.resolve(false, true, true))
        assertEquals(MicrophoneMode.FLOATING,
            MicrophoneModeResolver.resolve(false, false, true))
    }

    @Test fun recordingKeyboardTransitionsSwitchPositionsImmediately() {
        assertEquals(MicrophoneMode.RECORDING_DOCK,
            MicrophoneModeResolver.resolve(true, true, true))
        assertEquals(MicrophoneMode.FLOATING,
            MicrophoneModeResolver.resolve(false, true, true))
        assertEquals(MicrophoneMode.RECORDING_DOCK,
            MicrophoneModeResolver.resolve(true, true, true))
        assertEquals(MicrophoneMode.KEYBOARD,
            MicrophoneModeResolver.resolve(true, false, true))
    }

    @Test fun stoppingRecordingRestoresPositionOneWithoutTimer() {
        assertEquals(MicrophoneMode.KEYBOARD,
            MicrophoneModeResolver.resolve(true, false, true))
    }

    @Test fun calibrationCanShowAnyOfThreePositions() {
        for (mode in MicrophoneMode.entries) {
            assertEquals(mode,
                MicrophoneModeResolver.resolve(true, false, false, mode))
        }
    }
}

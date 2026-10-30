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

    @Test fun typingCanHideInsteadOfUsingPositionThree() {
        assertEquals(TypingAction.HIDE, TypingAction.fromPreference("hide"))
        org.junit.Assert.assertTrue(MicrophoneModeResolver.shouldHideForTyping(
            keyboardVisible = true, recording = false,
            suggestionsBusy = true, suggestionsDockEnabled = true,
            typingAction = TypingAction.HIDE
        ))
        org.junit.Assert.assertFalse(MicrophoneModeResolver.shouldHideForTyping(
            keyboardVisible = true, recording = true,
            suggestionsBusy = true, suggestionsDockEnabled = true,
            typingAction = TypingAction.HIDE
        ))
        org.junit.Assert.assertFalse(MicrophoneModeResolver.shouldHideForTyping(
            keyboardVisible = false, recording = false,
            suggestionsBusy = true, suggestionsDockEnabled = true,
            typingAction = TypingAction.HIDE
        ))
    }

    @Test fun typingCanChooseFourthSavedPosition() {
        assertEquals(MicrophoneMode.TYPING_DOCK,
            MicrophoneModeResolver.resolve(
                keyboardVisible = true, recording = false, recordingDockEnabled = true,
                suggestionsBusy = true, suggestionsDockEnabled = true,
                typingAction = TypingAction.POSITION_4
            ))
        assertEquals(MicrophoneMode.RECORDING_DOCK,
            MicrophoneModeResolver.resolve(
                keyboardVisible = true, recording = true, recordingDockEnabled = true,
                suggestionsBusy = true, suggestionsDockEnabled = true,
                typingAction = TypingAction.POSITION_4
            ))
    }

    @Test fun noSuggestionsRestoresNormalKeyboardLocation() {
        assertEquals(MicrophoneMode.KEYBOARD,
            MicrophoneModeResolver.resolve(
                keyboardVisible = true, recording = false, recordingDockEnabled = true,
                suggestionsBusy = false, suggestionsDockEnabled = true,
                typingAction = TypingAction.POSITION_4
            ))
    }

    @Test fun unknownPreferenceFallsBackToPositionThree() {
        assertEquals(TypingAction.POSITION_3, TypingAction.fromPreference("unknown"))
    }

    @Test fun calibrationCanShowAnyOfThreePositions() {
        for (mode in MicrophoneMode.entries) {
            assertEquals(mode,
                MicrophoneModeResolver.resolve(true, false, false, mode))
        }
    }
}

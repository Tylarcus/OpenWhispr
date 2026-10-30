package com.edib.openwhispr

import android.text.InputType
import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardLayoutProfileTest {
    @Test fun regularTextUsesTextProfile() {
        assertEquals(KeyboardLayoutProfile.TEXT,
            KeyboardLayoutClassifier.classify(InputType.TYPE_CLASS_TEXT, false))
    }

    @Test fun numericAndDecimalFieldsUseNumberProfile() {
        assertEquals(KeyboardLayoutProfile.NUMBER,
            KeyboardLayoutClassifier.classify(InputType.TYPE_CLASS_NUMBER, false))
        assertEquals(KeyboardLayoutProfile.NUMBER,
            KeyboardLayoutClassifier.classify(
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL, false))
        assertEquals(KeyboardLayoutProfile.NUMBER,
            KeyboardLayoutClassifier.classify(InputType.TYPE_CLASS_PHONE, false))
    }

    @Test fun htmlTextInputmodeNumericViaAriaLabelIsRecognized() {
        // Iron Shrine AMRAP currently uses type="text" inputmode="numeric".
        assertEquals(KeyboardLayoutProfile.NUMBER,
            KeyboardLayoutClassifier.classify(
                InputType.TYPE_CLASS_TEXT, false, "Enter completed reps"))
        assertEquals(KeyboardLayoutProfile.NUMBER,
            KeyboardLayoutClassifier.classify(
                InputType.TYPE_CLASS_TEXT, false, "Type completed repetitions"))
        assertEquals(KeyboardLayoutProfile.TEXT,
            KeyboardLayoutClassifier.classify(
                InputType.TYPE_CLASS_TEXT, false, "Search the web"))
    }

    @Test fun htmlPinLabelCanHideEvenIfEditorClaimsText() {
        assertEquals(KeyboardLayoutProfile.SECURE,
            KeyboardLayoutClassifier.classify(
                InputType.TYPE_CLASS_TEXT, false, "Enter your four digit PIN"))
    }

    @Test fun pinAndPasswordsUseSecureProfile() {
        assertEquals(KeyboardLayoutProfile.SECURE,
            KeyboardLayoutClassifier.classify(
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD, false))
        assertEquals(KeyboardLayoutProfile.SECURE,
            KeyboardLayoutClassifier.classify(
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD, false))
        assertEquals(KeyboardLayoutProfile.SECURE,
            KeyboardLayoutClassifier.classify(InputType.TYPE_CLASS_TEXT, true))
    }

    @Test fun numericDefaultsToHiddenUnlessConfigured() {
        assertEquals(NumericKeyboardBehavior.HIDE,
            NumericKeyboardBehavior.fromPreference(null))
        assertEquals(NumericKeyboardBehavior.SEPARATE,
            NumericKeyboardBehavior.fromPreference("separate"))
        assertEquals(NumericKeyboardBehavior.SHARED,
            NumericKeyboardBehavior.fromPreference("shared"))
    }
}

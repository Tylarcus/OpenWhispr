package com.edib.openwhispr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayPlacementTest {
    @Test fun keyboardModeCanStorePositionAboveKeyboard() {
        // The keyboard starts at y=1450, but an idle mic is allowed at y=250.
        val saved = OverlayPlacement.remember(640, 250, 1080, 2340, 48)
        val result = OverlayPlacement.restore(saved, 1080, 2340, 48, 8)
        assertTrue(kotlin.math.abs(640 - result.x) <= 1)
        assertTrue(kotlin.math.abs(250 - result.y) <= 1)
        assertTrue(result.y < 1450)
    }

    @Test fun coordinatesDoNotDependOnKeyboardTop() {
        val saved = OverlayPlacement.remember(900, 1200, 1080, 2340, 56)
        val restored = OverlayPlacement.restore(saved, 1080, 2340, 56, 8)
        assertTrue(kotlin.math.abs(900 - restored.x) <= 1)
        assertTrue(kotlin.math.abs(1200 - restored.y) <= 1)
    }

    @Test fun resizedOverlayStaysOnScreen() {
        val fractions = OverlayPlacement.Fractions(0.95f, 0.99f)
        val restored = OverlayPlacement.restore(fractions, 1080, 2340, 540, 8)
        assertTrue(restored.x + 540 <= 1080)
        assertTrue(restored.y + 540 <= 2340)
    }

    @Test fun zeroMarginAllowsExactScreenEdges() {
        val farRight = OverlayPlacement.restore(
            OverlayPlacement.Fractions(1f, 1f), 1080, 2340, 80, 0
        )
        assertEquals(1000, farRight.x)
        assertEquals(2260, farRight.y)
        val farLeft = OverlayPlacement.restore(
            OverlayPlacement.Fractions(0f, 0f), 1080, 2340, 80, 0
        )
        assertEquals(0, farLeft.x)
        assertEquals(0, farLeft.y)
    }

    @Test fun invalidFractionsAreClamped() {
        val restored = OverlayPlacement.restore(OverlayPlacement.Fractions(5f, -3f), 1080, 2340, 40, 8)
        assertEquals(1032, restored.x)
        assertEquals(8, restored.y)
    }
}

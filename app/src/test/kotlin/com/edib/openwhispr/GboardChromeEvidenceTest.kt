package com.edib.openwhispr

import org.junit.Assert.*
import org.junit.Test

class GboardChromeEvidenceTest {
    @Test fun distinguishCandidateItemsFromAlwaysVisibleContainers() {
        assertTrue(GboardChromeEvidence.isCandidateContainerId(
            "com.google.android.inputmethod.latin:id/suggestion_strip"))
        assertFalse(GboardChromeEvidence.isCandidateItemId(
            "com.google.android.inputmethod.latin:id/suggestion_strip"))
        assertTrue(GboardChromeEvidence.isCandidateItemId(
            "com.google.android.inputmethod.latin:id/candidate_view"))
    }

    @Test fun recognizeStaticGboardShortcutsWithoutReadingSuggestedWords() {
        assertEquals("clipboard", GboardChromeEvidence.toolbarToken(null, "Clipboard"))
        assertEquals("gif", GboardChromeEvidence.toolbarToken(null, "GIF"))
        assertEquals("voice", GboardChromeEvidence.toolbarToken(
            "com.google.android.inputmethod.latin:id/voice_typing_button", null))
        assertNull(GboardChromeEvidence.toolbarToken(null, "synchronization"))
        assertNull(GboardChromeEvidence.toolbarToken(null, "my private sentence"))
    }

    @Test fun requireTwoDistinctControlsAndNoCandidates() {
        val buttons = setOf("clipboard", "gif", "voice")
        assertTrue(GboardChromeEvidence.showsPlainToolbar(false, buttons))
        assertFalse(GboardChromeEvidence.showsPlainToolbar(true, buttons))
        assertFalse(GboardChromeEvidence.showsPlainToolbar(false, setOf("gif")))
    }
}

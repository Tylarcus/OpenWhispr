package com.edib.openwhispr

/**
 * Best-effort, metadata-only keyboard suggestion detector state.
 * No word contents or text-field data are retained.
 *
 * On Gboard builds exposing candidate/suggestion container view IDs, the
 * detector follows their visibility. Other builds use a sticky text-edit
 * fallback: after typing, stay out of the suggestion row until the keyboard
 * closes or the user changes the focused editor.
 */
class SuggestionDockState {
    companion object {
        /** Match stable accessibility view IDs, not private suggestion text. */
        fun isCandidateStripId(id: String?): Boolean {
            val normalized = id?.lowercase() ?: return false
            return normalized.contains("suggestion_strip") ||
                normalized.contains("candidate_strip") ||
                normalized.contains("suggestion_list") ||
                normalized.contains("candidate_view")
        }
    }
    private var hasRecognizableStrip = false
    private var stripVisible = false
    private var editedInCurrentField = false

    fun onEdit() {
        // A new genuine edit overrides a previously observed idle toolbar.
        // Only subsequent Gboard structure events can clear it again.
        editedInCurrentField = true
        hasRecognizableStrip = false
        stripVisible = false
    }

    fun onRecognizedStructure(suggestionStripVisible: Boolean) {
        hasRecognizableStrip = true
        stripVisible = suggestionStripVisible
    }

    fun onPlainToolbarVisible() {
        // Explicit signal that Gboard switched from word candidates back to
        // clipboard/GIF/voice controls while the keyboard is still OPEN.
        hasRecognizableStrip = true
        stripVisible = false
        editedInCurrentField = false
    }

    fun onEditorChanged() {
        editedInCurrentField = false
        hasRecognizableStrip = false
        stripVisible = false
    }

    fun onKeyboardClosed() {
        hasRecognizableStrip = false
        stripVisible = false
        editedInCurrentField = false
    }

    val shouldUseAlternativePosition: Boolean
        get() = if (hasRecognizableStrip) stripVisible else editedInCurrentField

    val detectionMethod: String
        get() = if (hasRecognizableStrip) "Gboard structure" else "text edit fallback"
}

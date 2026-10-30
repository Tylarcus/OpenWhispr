package com.edib.openwhispr

import java.util.Locale

/**
 * Interpret only FIXED Gboard UI chrome; never inspect the contents of
 * suggestions, clipboard items, editable fields, or user-entered text.
 *
 * Gboard does not promise a public suggestion-state API. These heuristics
 * apply only when its accessibility hierarchy exposes recognizable controls.
 */
object GboardChromeEvidence {
    const val PACKAGE_NAME = "com.google.android.inputmethod.latin"

    fun isCandidateItemId(id: String?): Boolean {
        val name = id?.substringAfterLast('/')?.lowercase(Locale.ROOT) ?: return false
        return listOf("candidate_view", "candidate_item", "candidate_word",
            "suggestion_word", "suggestion_item", "suggestion_button",
            "word_suggestion", "suggested_word").any { it in name }
    }

    fun isCandidateContainerId(id: String?): Boolean {
        val name = id?.substringAfterLast('/')?.lowercase(Locale.ROOT) ?: return false
        return "suggestion_strip" in name || "candidate_strip" in name ||
            "suggestion_list" in name || "candidate_list" in name
    }

    /**
     * Classify recognizable fixed shortcut buttons at the top of Gboard.
     * Description comparisons are strict whitelists: arbitrary text is never
     * stored, logged, or considered a word prediction.
     */
    fun toolbarToken(viewId: String?, contentDescription: CharSequence?): String? {
        val id = viewId?.substringAfterLast('/')?.lowercase(Locale.ROOT).orEmpty()
        val label = contentDescription?.toString()?.trim()?.lowercase(Locale.ROOT).orEmpty()
        return when {
            "clipboard" in id || label in setOf("clipboard", "open clipboard", "clipboard manager") -> "clipboard"
            id == "gif" || "gif_button" in id ||
                label in setOf("gif", "gifs", "search gifs") -> "gif"
            "toolbar_toggle" in id || "toolbar_button" in id ||
                label in setOf("open toolbar", "show toolbar", "more options", "tools") -> "toolbar"
            "emoji_button" in id || "emoji_search" in id ||
                label in setOf("emoji", "emojis", "emoji search") -> "emoji"
            "voice_typing" in id || "voice_input" in id ||
                label in setOf("voice typing", "voice input", "voice type") -> "voice"
            "translate" in id || label in setOf("translate", "google translate") -> "translate"
            "settings" in id || label == "settings" -> "settings"
            "text_editing" in id || label in setOf("text editing", "edit text") -> "editing"
            else -> null
        }
    }

    /** Two distinct shortcut buttons are strong evidence of the plain toolbar. */
    fun showsPlainToolbar(candidateItemVisible: Boolean, tokens: Set<String>): Boolean =
        !candidateItemVisible && tokens.size >= 2
}

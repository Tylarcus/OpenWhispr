package com.edib.openwhispr

/**
 * Positions are fractions of the WHOLE display, not the keyboard viewport.
 * An idle dock can therefore be dragged above Gboard without snapping down.
 * Keyboard-visible and keyboard-hidden positions are saved under separate keys.
 */
object OverlayPlacement {
    data class Fractions(val x: Float, val y: Float)

    fun remember(x: Int, y: Int, screenWidth: Int, screenHeight: Int, size: Int): Fractions {
        val horizontal = (screenWidth - size).coerceAtLeast(1)
        val vertical = (screenHeight - size).coerceAtLeast(1)
        return Fractions(
            (x.toFloat() / horizontal).coerceIn(0f, 1f),
            (y.toFloat() / vertical).coerceIn(0f, 1f)
        )
    }

    fun restore(
        fractions: Fractions,
        screenWidth: Int,
        screenHeight: Int,
        size: Int,
        margin: Int
    ): BubbleSize.Position {
        val width = (screenWidth - size).coerceAtLeast(0)
        val height = (screenHeight - size).coerceAtLeast(0)
        val x = (fractions.x.coerceIn(0f, 1f) * width).toInt()
        val y = (fractions.y.coerceIn(0f, 1f) * height).toInt()
        return BubbleSize.clampPosition(x, y, screenWidth, screenHeight, size, margin)
    }
}

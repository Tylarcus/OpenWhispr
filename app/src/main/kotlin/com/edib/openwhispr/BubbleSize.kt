package com.edib.openwhispr

import kotlin.math.roundToInt

object BubbleSize {
    const val PREFERENCE_KEY = "bubble_size_percent"
    const val MIN_PERCENT = 5
    const val DEFAULT_PERCENT = 100
    const val MAX_PERCENT = 1000
    const val STEP_PERCENT = 5

    const val BUTTON_DP = 44
    const val RING_DP = 56
    const val PADDING_DP = 10
    private const val TAP_THRESHOLD_DP = 10
    private const val FEEDBACK_OFFSET_DP = 64

    data class Dimensions(
        val buttonDp: Int,
        val ringDp: Int,
        val paddingDp: Int,
        val tapThresholdDp: Int,
        val feedbackOffsetDp: Int
    )

    data class Position(val x: Int, val y: Int)

    fun preferencePercent(value: Int?): Int =
        if (value != null && value in MIN_PERCENT..MAX_PERCENT &&
            (value - MIN_PERCENT) % STEP_PERCENT == 0
        ) {
            value
        } else {
            DEFAULT_PERCENT
        }

    fun sliderPercent(value: Int): Int {
        val bounded = value.coerceIn(MIN_PERCENT, MAX_PERCENT)
        return MIN_PERCENT + ((bounded - MIN_PERCENT + STEP_PERCENT / 2) / STEP_PERCENT) * STEP_PERCENT
    }

    fun dimensions(percent: Int): Dimensions {
        val scale = percent.coerceIn(MIN_PERCENT, MAX_PERCENT) / 100f
        fun scaled(value: Int) = (value * scale).roundToInt()

        return Dimensions(
            buttonDp = maxOf(1, scaled(BUTTON_DP)),
            ringDp = maxOf(1, scaled(RING_DP)),
            paddingDp = scaled(PADDING_DP),
            tapThresholdDp = TAP_THRESHOLD_DP,
            feedbackOffsetDp = scaled(FEEDBACK_OFFSET_DP)
        )
    }

    /** Maximum overlay ring diameter is half the display width, in physical pixels. */
    fun maxPercentForScreen(screenWidthPx: Int, density: Float): Int {
        val halfWidthDp = screenWidthPx / (2f * density)
        val raw = (halfWidthDp * 100f / RING_DP).toInt()
        return (raw / STEP_PERCENT * STEP_PERCENT).coerceIn(MIN_PERCENT, MAX_PERCENT)
    }

    fun valueLabel(
        percent: Int,
        standard: String = "standard",
        smaller: String = "smaller",
        larger: String = "larger",
    ): String = when (percent) {
        DEFAULT_PERCENT -> "$percent% · $standard"
        in MIN_PERCENT until DEFAULT_PERCENT -> "$percent% · $smaller"
        else -> "$percent% · $larger"
    }

    fun accessibilityDescription(
        percent: Int,
        standard: String = "standard size",
        smaller: String = "smaller than standard",
        larger: String = "larger than standard",
        percentUnit: String = "percent",
    ): String = when (percent) {
        DEFAULT_PERCENT -> "$percent $percentUnit, $standard"
        in MIN_PERCENT until DEFAULT_PERCENT -> "$percent $percentUnit, $smaller"
        else -> "$percent $percentUnit, $larger"
    }

    fun clampPosition(
        x: Int,
        y: Int,
        screenWidth: Int,
        screenHeight: Int,
        ringSize: Int,
        margin: Int
    ): Position {
        val maxX = maxOf(margin, screenWidth - ringSize - margin)
        val maxY = maxOf(margin, screenHeight - ringSize - margin)
        return Position(x.coerceIn(margin, maxX), y.coerceIn(margin, maxY))
    }

    // Retained for compatibility with older builds; V2 no longer snaps drags.
    fun snappedX(x: Int, ringSize: Int, screenWidth: Int, margin: Int): Int {
        val maxX = maxOf(margin, screenWidth - ringSize - margin)
        return if (x + ringSize / 2 > screenWidth / 2) maxX else margin
    }

    const val POSITION_PERCENT_KEY = "overlay_position_percent"
    const val ON_LEFT_KEY = "overlay_on_left"
    const val DEFAULT_POSITION_PERCENT = 50

    fun positionPercent(value: Int?): Int = (value ?: DEFAULT_POSITION_PERCENT).coerceIn(0, 100)

    /** Where the bubble sits for a saved setting: [percent] is the share of the
     * available height (0 = top edge, 100 = bottom edge, margins included), so
     * the whole ring always stays on screen. */
    fun positionFor(
        percent: Int,
        onLeft: Boolean,
        screenWidth: Int,
        screenHeight: Int,
        ringSize: Int,
        margin: Int
    ): Position {
        val maxY = maxOf(margin, screenHeight - ringSize - margin)
        val y = margin + ((maxY - margin) * positionPercent(percent) / 100f).roundToInt()
        val x = if (onLeft) margin else screenWidth - ringSize - margin
        return clampPosition(x, y, screenWidth, screenHeight, ringSize, margin)
    }
}

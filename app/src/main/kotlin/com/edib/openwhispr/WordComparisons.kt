package com.edib.openwhispr

/**
 * User-approved word comparisons. Exact reference counts and approximate
 * analogies are described with their necessary qualifiers in the text.
 * Offline-only, display-only: no overlay, notification, audio, or timers.
 */
object WordComparisons {
    data class Milestone(val words: Long, val description: String)
    val milestones: List<Milestone> = listOf(
        Milestone(10L, "You said enough for a quick reply."),
        Milestone(25L, "About a thoughtful little text message."),
        Milestone(40L, "Roughly a compact social post."),
        Milestone(60L, "A whole paragraph's worth of ideas."),
        Milestone(80L, "A short thank-you note."),
        Milestone(100L, "A substantial message without touching your keyboard."),
        Milestone(125L, "About a minute of relaxed speaking."),
        Milestone(150L, "About one minute of ordinary dictation."),
        Milestone(175L, "Enough for a short personal email."),
        Milestone(200L, "A multi-paragraph explanation."),
        Milestone(250L, "About one double-spaced manuscript page."),
        Milestone(272L, "The length of the Gettysburg Address."),
        Milestone(350L, "Around a page and a half of double-spaced text."),
        Milestone(500L, "Around two double-spaced pages."),
        Milestone(650L, "A compact personal essay."),
        Milestone(769L, "Roughly the entire word count of Green Eggs and Ham."),
        Milestone(1000L, "The size of a short essay."),
        Milestone(1250L, "Roughly five double-spaced pages."),
        Milestone(1500L, "Six double-spaced pages."),
        Milestone(2000L, "Eight double-spaced pages."),
        Milestone(2500L, "Ten double-spaced pages."),
        Milestone(3000L, "About 20 minutes of talking at 150 WPM."),
        Milestone(4000L, "Sixteen double-spaced pages."),
        Milestone(5000L, "About 33 minutes at 150 WPM."),
        Milestone(7500L, "About fifty minutes at 150 WPM."),
        Milestone(10000L, "Forty double-spaced pages."),
        Milestone(15000L, "One hundred minutes of words at 150 WPM."),
        Milestone(20000L, "Eighty double-spaced pages."),
        Milestone(30000L, "Around a short novella-length draft."),
        Milestone(40000L, "About 160 double-spaced pages."),
        Milestone(50000L, "About 200 double-spaced pages."),
        Milestone(76944L, "Around the length of Harry Potter and the Philosopher's Stone."),
        Milestone(100000L, "Enough words to fill about 400 double-spaced pages."),
    )

    fun forCount(words: Long): Milestone? =
        milestones.lastOrNull { words >= it.words }

    fun description(words: Long): String = if (words <= 0L) {
        "Once you dictate something, you'll see how your words compare."
    } else {
        val reached = forCount(words)
        if (reached == null) "You're getting started — each word adds up."
        else reached.description
    }
}

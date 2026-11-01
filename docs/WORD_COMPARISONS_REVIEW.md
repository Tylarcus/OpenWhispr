# OpenWhispr beta — word-comparison editorial review
**Status: USER-APPROVED for in-app Insights only — November 1, 2026.**

These are approved **word-count comparisons** for Today, This Week (Monday–Sunday), Lifetime and Last Dictation in the separately installed redesign beta. Each comparison is a playful approximation unless labelled an exact reference. No transcription text or audio is stored by the word counter.

| Words | Display suggestion | Evidence / honesty note |
|---:|---|---|
| 10 | You said enough for a quick reply. | Illustrative |
| 25 | About a thoughtful little text message. | Illustrative; messages vary |
| 40 | Roughly a compact social post. | A regular X post is 280 characters, **not 280 words**; depends on word length |
| 60 | A whole paragraph's worth of ideas. | Illustrative |
| 80 | A short thank-you note. | Illustrative |
| 100 | A substantial message without touching your keyboard. | Illustrative |
| 125 | About a minute of relaxed speaking. | Assumes an illustrative 125 WPM |
| 150 | About one minute of ordinary dictation. | Assumes an illustrative 150 WPM |
| 175 | Enough for a short personal email. | Illustrative |
| 200 | A multi-paragraph explanation. | Illustrative |
| 250 | About one double-spaced manuscript page. | Approximate, layout dependent |
| 272 | The length of the Gettysburg Address. | 272 words — U.S. National Park Service |
| 350 | Around a page and a half of double-spaced text. | Approximate at 250 words/page |
| 500 | Around two double-spaced pages. | Approximate |
| 650 | A compact personal essay. | Illustrative |
| 769 | Roughly the entire word count of *Green Eggs and Ham*. | 769 total words — TeachingBooks. It uses only 50 **distinct** words |
| 1,000 | The size of a short essay. | Illustrative |
| 1,250 | Roughly five double-spaced pages. | Approximate |
| 1,500 | Six double-spaced pages. | Approximate |
| 2,000 | Eight double-spaced pages. | Approximate |
| 2,500 | Ten double-spaced pages. | Approximate |
| 3,000 | About 20 minutes of talking at 150 WPM. | Calculated |
| 4,000 | Sixteen double-spaced pages. | Approximate |
| 5,000 | About 33 minutes at 150 WPM. | Calculated |
| 7,500 | About fifty minutes at 150 WPM. | Calculated |
| 10,000 | Forty double-spaced pages. | Approximate |
| 15,000 | One hundred minutes of words at 150 WPM. | Calculated |
| 20,000 | Eighty double-spaced pages. | Approximate |
| 30,000 | Around a short novella-length draft. | Approximate; genre definitions vary |
| 40,000 | About 160 double-spaced pages. | Approximate |
| 50,000 | About 200 double-spaced pages. | Approximate |
| 76,944 | Around the length of *Harry Potter and the Philosopher's Stone*. | Harry Potter Lexicon (UK edition), word counts vary by edition |
| 100,000 | Enough words to fill about 400 double-spaced pages. | Approximate |

## Primary / verifiable sources
- X's 280-character (not word) limit: https://help.x.com/en/using-x/how-to-post
- Gettysburg Address word count: https://www.nps.gov/foth/lincoln-s-legacy-the-eloquent-president.htm
- *Green Eggs and Ham*: https://library.teachingbooks.net/tb.cgi/tb.cgi?tid=2850
- *Harry Potter and the Philosopher's Stone*: https://www.hp-lexicon.org/source/the-harry-potter-novels/ps/

## Approved interaction requirements — November 1, 2026
- Display ONE comparison card on the **Insights tab** while the app is open. Never show comparison overlays, toast messages, system notifications, or popup interruptions during dictation or typing.
- Selectable scopes: **Today**, **This Week** (Monday–Sunday), **Lifetime**, **Last Dictation**.
- Display the approximate comparison matching the most recently reached word-count threshold. No next-milestone progress bars, no milestone notification feed, no intrusive timer rotation.
- When more comparison variants for the same general threshold are curated and approved, we may rotate them while Insights is visible only. That is NOT part of this initial implementation.
- Offer a Settings switch to disable the comparison card without deleting word/WPM counters.
- All comparisons remain bundled and offline. No external services.

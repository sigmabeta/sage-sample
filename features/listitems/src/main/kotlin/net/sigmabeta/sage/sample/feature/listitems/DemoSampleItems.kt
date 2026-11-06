package net.sigmabeta.sage.sample.feature.listitems

import java.util.Random
import net.sigmabeta.sage.ui.StringGenerator

/**
 * Builds generated demo rows for the List Items screens.
 *
 * Every call starts from the same [DEMO_ITEM_SEED], so a screen's content is random-looking but
 * identical on every visit — popping and re-pushing a screen never changes its rows. Screens that
 * need more rows than another extend the prefix rather than re-seeding.
 */
internal fun demoSampleItems(count: Int): List<SampleItem> {
    val random = Random(DEMO_ITEM_SEED)
    val generator = StringGenerator(random)

    return List(count) { index ->
        SampleItem(
            id = index + 1L,
            name = generator.generateName(),
            caption = generateCaption(generator, random),
        )
    }
}

/**
 * Three to five lorem words, using [generator] and the same [random] sequence as the names.
 *
 * `generateLorem` returns between one and 49 words, so the loop collects enough words for the
 * requested caption rather than assuming a single call is long enough.
 */
private fun generateCaption(generator: StringGenerator, random: Random): String {
    val wordCount = CAPTION_MIN_WORDS +
        random.nextInt(CAPTION_MAX_WORDS - CAPTION_MIN_WORDS + 1)
    val words = mutableListOf<String>()

    while (words.size < wordCount) {
        words += generator.generateLorem().split(' ')
    }

    return words.take(wordCount).joinToString(" ")
}

private const val CAPTION_MIN_WORDS = 3
private const val CAPTION_MAX_WORDS = 5

/** Static seed: the generated content is random-looking but reproducible. */
internal const val DEMO_ITEM_SEED = 20240517L

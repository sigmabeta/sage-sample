package net.sigmabeta.sage.sample.feature.listitems

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.util.Random
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.di.AppScope
import net.sigmabeta.sage.ui.StringGenerator

/**
 * The sample's generated datasource for List Items.
 *
 * Every load builds a fresh [Random] from [RANDOM_SEED], so the 25 rows are random in content but
 * identical on every visit — the screen looks the same after popping and re-pushing. The
 * artificial latency comes from the shared `runInBackground` seam, which applies the duration
 * selected on the Settings screen; this repository does not add a delay of its own.
 *
 * Metro binds it to [SampleItemRepository] once for the application graph; the sample has no real
 * backend.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class DemoSampleItemRepository : SampleItemRepository {
    override fun load(): Flow<LCE<List<SampleItem>>> = flow {
        val random = Random(RANDOM_SEED)
        val generator = StringGenerator(random)

        emit(
            LCE.Content(
                List(ITEM_COUNT) { index ->
                    SampleItem(
                        id = index + 1L,
                        name = generator.generateName(),
                        caption = generateCaption(generator, random),
                    )
                },
            ),
        )
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

    private companion object {
        const val ITEM_COUNT = 25
        const val CAPTION_MIN_WORDS = 3
        const val CAPTION_MAX_WORDS = 5

        /** Static seed: the generated content is random-looking but reproducible. */
        const val RANDOM_SEED = 20240517L
    }
}

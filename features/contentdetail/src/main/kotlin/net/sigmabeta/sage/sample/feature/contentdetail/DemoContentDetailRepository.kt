package net.sigmabeta.sage.sample.feature.contentdetail

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.util.Random
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.di.AppScope
import net.sigmabeta.sage.ui.Icon
import net.sigmabeta.sage.ui.StringGenerator

/**
 * The sample's generated datasource for Content detail.
 *
 * Every load rebuilds the same content through [demoContentDetail], so the screen is random in
 * content but identical on every visit — popping and re-pushing it never changes the title,
 * metadata, actions, related items or parts. The artificial latency comes from the shared
 * `runInBackground` seam, which applies the duration selected on the Settings screen; this
 * repository does not add a delay of its own.
 *
 * Metro binds it to [ContentDetailRepository] once for the application graph; the sample has no
 * real content backend.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class DemoContentDetailRepository : ContentDetailRepository {
    override fun load(): Flow<LCE<ContentDetail>> = flow {
        emit(LCE.Content(demoContentDetail()))
    }
}

/**
 * Builds the detail screen's content from [CONTENT_DETAIL_SEED].
 *
 * The seed is deliberately fixed rather than drawn per visit: this is the "randomized content,
 * predetermined seed" contract, so a demo screenshot and a re-push of the screen always show the
 * same item. Changing the seed changes every generated field.
 */
internal fun demoContentDetail(seed: Long = CONTENT_DETAIL_SEED): ContentDetail {
    val random = Random(seed)
    val generator = StringGenerator(random)

    return ContentDetail(
        id = CONTENT_DETAIL_ID,
        title = generator.generateTitle(),
        details = List(DETAILS_COUNT) {
            ContentDetailField(
                label = generator.generateTitle(),
                value = generator.generateName(),
            )
        },
        actions = List(ACTION_COUNT) {
            ContentAction(
                label = generator.generateTitle(),
                icon = CTA_ICONS[random.nextInt(CTA_ICONS.size)],
            )
        },
        related = List(RELATED_COUNT) { index ->
            RelatedContent(
                id = index + 1L,
                name = generator.generateTitle(),
            )
        },
        items = List(ITEM_COUNT) { index ->
            ContentItem(
                id = index + 1L,
                name = generator.generateTitle(),
                caption = generator.generateName(),
            )
        },
    )
}

/**
 * Static seed: the generated detail is random-looking but reproducible.
 *
 * It is intentionally unrelated to the list, grid and search seeds so each screen has its own
 * content while every screen remains deterministic.
 */
internal const val CONTENT_DETAIL_SEED = 20260911L

private const val CONTENT_DETAIL_ID = 1L
private const val DETAILS_COUNT = 4
private const val ACTION_COUNT = 4
private const val RELATED_COUNT = 5
private const val ITEM_COUNT = 8

/**
 * The icons a generated action may draw. Deliberately generic library/navigation icons rather
 * than a fixed play/shuffle/favorite vocabulary, so the sample does not read as a media app.
 */
private val CTA_ICONS = listOf(
    Icon.Album,
    Icon.Browse,
    Icon.Calendar,
    Icon.Chip,
    Icon.Computer,
    Icon.Description,
    Icon.Difficulty,
    Icon.Edit,
    Icon.Home,
    Icon.Library,
    Icon.MusicNote,
    Icon.Person,
    Icon.Play,
    Icon.Plus,
    Icon.Refresh,
    Icon.Repeat,
    Icon.Save,
    Icon.Search,
    Icon.Tag,
    Icon.Visibility,
    Icon.Warning,
)

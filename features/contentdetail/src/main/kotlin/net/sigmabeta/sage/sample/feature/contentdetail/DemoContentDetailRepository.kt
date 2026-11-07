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
 * Content is generated from the requested [ContentId], so two IDs show different items while the
 * same ID reproduces exactly on every visit — popping and re-pushing a screen never changes its
 * title, metadata, actions, related items or parts. An ID outside the sample's known surfaces is
 * the explicit not-found result (`LCE.Content(null)`), not an error.
 *
 * The artificial latency comes from the shared `runInBackground` seam, which applies the duration
 * selected on the Settings screen; this repository does not add a delay of its own.
 *
 * Metro binds it to [ContentDetailRepository] once for the application graph; the sample has no
 * real content backend.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class DemoContentDetailRepository : ContentDetailRepository {
    override fun load(contentId: ContentId): Flow<LCE<ContentDetail?>> = flow {
        emit(LCE.Content(demoContentDetail(contentId)))
    }
}

/**
 * Builds the detail screen's content for [contentId].
 *
 * The ID seeds the generator rather than a fixed constant: the content is still "randomized
 * content, predetermined seed" per address, so a demo screenshot and a re-push of the same link
 * always show the same item while a different link shows a different one.
 */
internal fun demoContentDetail(contentId: ContentId): ContentDetail? {
    if (!isKnownContentId(contentId)) return null

    val random = Random(contentIdSeed(contentId))
    val generator = StringGenerator(random)

    return ContentDetail(
        id = contentId,
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
 * A stable seed derived from the ID's text.
 *
 * `String.hashCode()` is specified on the JVM but is only 32 bits; this local 64-bit mixing keeps
 * two nearby IDs (`list-items:7`, `list-items:8`) from landing on similar generated content while
 * staying deterministic across runs and platforms.
 */
internal fun contentIdSeed(contentId: ContentId): Long {
    var hash = FNV_OFFSET_BASIS
    contentId.value.forEach { char ->
        hash = hash xor char.code.toLong()
        hash *= FNV_PRIME
    }
    return hash
}

private const val DETAILS_COUNT = 4
private const val ACTION_COUNT = 4
private const val RELATED_COUNT = 5
private const val ITEM_COUNT = 8

/** FNV-1a's 64-bit offset basis and prime. */
private const val FNV_OFFSET_BASIS = -0x340d631b7bdddcdbL
private const val FNV_PRIME = 0x100000001b3L

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

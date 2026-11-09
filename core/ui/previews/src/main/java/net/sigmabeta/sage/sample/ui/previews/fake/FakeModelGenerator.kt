package net.sigmabeta.sage.sample.ui.previews.fake

import java.util.Random
import net.sigmabeta.sage.sample.models.ContentAction
import net.sigmabeta.sage.sample.models.ContentDetail
import net.sigmabeta.sage.sample.models.ContentDetailField
import net.sigmabeta.sage.sample.models.ContentId
import net.sigmabeta.sage.sample.models.ContentItem
import net.sigmabeta.sage.sample.models.HomeItem
import net.sigmabeta.sage.sample.models.RelatedContent
import net.sigmabeta.sage.sample.models.SampleItem
import net.sigmabeta.sage.sample.models.SearchResult
import net.sigmabeta.sage.ui.Icon
import net.sigmabeta.sage.ui.StringGenerator

/**
 * Deterministic domain-model generator for screenshot previews — the sample's analogue of
 * Chipbox's `FakeModelGenerator`.
 *
 * Self-contained: given a [seed], every call produces identical [SampleItem], [SearchResult],
 * [HomeItem] and [ContentDetail] data, so Paparazzi snapshots are stable across runs. It owns its
 * [Random]/[StringGenerator] so feature fixtures never need a direct dependency on SAGE's
 * generator.
 */
@Suppress("MagicNumber")
class FakeModelGenerator(private val seed: Long = DEFAULT_SEED) {
    private val random = Random(seed)
    private val stringGenerator = StringGenerator(random)

    fun loadingName(): String {
        random.setSeed(seed)
        return stringGenerator.generateName()
    }

    fun randomSampleItems(count: Int = SAMPLE_ITEM_COUNT): List<SampleItem> {
        random.setSeed(seed + 1)
        return List(count) { index ->
            SampleItem(
                id = index + 1L,
                name = stringGenerator.generateName(),
                caption = stringGenerator.generateName(),
            )
        }
    }

    fun randomSearchResults(
        prefix: String,
        count: Int = SEARCH_RESULT_COUNT,
    ): List<SearchResult> {
        random.setSeed(seed + 2)
        return List(count) { index ->
            SearchResult(
                id = index + 1L,
                name = "$prefix ${stringGenerator.generateTitle()}",
                caption = stringGenerator.generateName(),
            )
        }
    }

    fun randomHomeItems(count: Int = HOME_ITEM_COUNT): List<HomeItem> {
        random.setSeed(seed + 3)
        return List(count) { index ->
            HomeItem(
                id = index + 1L,
                title = stringGenerator.generateTitle(),
            )
        }
    }

    /**
     * A fully populated Content detail fixture: hero title, metadata, CTAs, related items and
     * part rows. [id] is only the stable identity; the generated values do not depend on it.
     */
    fun randomContentDetail(
        id: ContentId = ContentId("screenshot:content-detail"),
    ): ContentDetail {
        random.setSeed(seed + 4)
        return ContentDetail(
            id = id,
            title = stringGenerator.generateTitle(),
            details = List(DETAIL_COUNT) {
                ContentDetailField(
                    label = stringGenerator.generateTitle(),
                    value = stringGenerator.generateName(),
                )
            },
            actions = List(ACTION_COUNT) {
                ContentAction(
                    label = stringGenerator.generateTitle(),
                    icon = CTA_ICONS[random.nextInt(CTA_ICONS.size)],
                )
            },
            related = List(RELATED_COUNT) { index ->
                RelatedContent(
                    id = index + 1L,
                    name = stringGenerator.generateTitle(),
                )
            },
            items = List(CONTENT_ITEM_COUNT) { index ->
                ContentItem(
                    id = index + 1L,
                    name = stringGenerator.generateTitle(),
                    caption = stringGenerator.generateName(),
                )
            },
        )
    }

    private companion object {
        const val DEFAULT_SEED = 1234L
        const val SAMPLE_ITEM_COUNT = 12
        const val SEARCH_RESULT_COUNT = 4
        const val HOME_ITEM_COUNT = 5
        const val DETAIL_COUNT = 4
        const val ACTION_COUNT = 2
        const val RELATED_COUNT = 5
        const val CONTENT_ITEM_COUNT = 6

        /** Generic library/navigation icons, matching the demo repository's vocabulary. */
        val CTA_ICONS = listOf(
            Icon.Album,
            Icon.Browse,
            Icon.Calendar,
            Icon.Play,
            Icon.Person,
            Icon.Search,
            Icon.Tag,
        )
    }
}

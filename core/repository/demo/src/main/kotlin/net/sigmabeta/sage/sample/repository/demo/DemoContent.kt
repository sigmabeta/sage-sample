package net.sigmabeta.sage.sample.repository.demo

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
import net.sigmabeta.sage.sample.models.contentItemContentId
import net.sigmabeta.sage.sample.models.isKnownContentId
import net.sigmabeta.sage.sample.models.relatedContentId
import net.sigmabeta.sage.sample.repository.GRID_OF_IMAGES_ITEM_COUNT
import net.sigmabeta.sage.sample.repository.MAX_SEARCH_RESULT_COUNT
import net.sigmabeta.sage.sample.repository.MIN_SEARCH_QUERY_LENGTH
import net.sigmabeta.sage.sample.repository.MIN_SEARCH_RESULT_COUNT
import net.sigmabeta.sage.sample.repository.SearchResultKind
import net.sigmabeta.sage.sample.repository.normalizeSearchQuery
import net.sigmabeta.sage.sample.repository.searchSeedFor
import net.sigmabeta.sage.ui.Icon
import net.sigmabeta.sage.ui.StringGenerator

/**
 * The deterministic content the sample shows when no remote server is involved.
 *
 * Every function is pure: the same seed/index/query reproduces the same data on the Android app,
 * the desktop app, and `apps:server`. The repositories wrap these functions with the Settings
 * demo outcome (content/empty/error); the server calls them directly and always returns content,
 * because the outcome is a client-side simulation switch.
 */
object DemoContent {
    /** Total number of Home items available to a module seed's daily pick. */
    fun homeItemCount(seed: String): Int = homeCatalog(seed).size

    /** The Home item at [index] for [seed], or null past the end of the catalog. */
    fun homeItem(seed: String, index: Int): HomeItem? = homeCatalog(seed).getOrNull(index)

    /** The first [limit] Home items for [seed]. */
    fun homeGrid(seed: String, limit: Int): List<HomeItem> = homeCatalog(seed).take(limit)

    /** The detail for [contentId], or null when it names an unknown surface. */
    fun contentDetail(contentId: ContentId): ContentDetail? = demoContentDetail(contentId)

    /** The `[offset, offset + limit)` window of the List Items / Grid catalogue. */
    fun sampleItems(offset: Int, limit: Int, total: Int): List<SampleItem> =
        demoSampleItemsWindow(offset = offset, limit = limit, total = total)

    /** Deterministic search results for one query + result-set pair. */
    fun searchResults(query: String, kind: SearchResultKind): List<SearchResult> =
        searchResultsFor(query, kind)

    /** The results for a [searchSeedFor] seed; the route ID carries the seed rather than the query. */
    fun searchResultsForSeed(seed: Int): List<SearchResult> = searchResultsForSeedImpl(seed)

    private fun homeCatalog(seed: String): List<HomeItem> {
        val generator = StringGenerator(Random(seed.hashCode().toLong()))
        return List(HOME_CATALOG_SIZE) { index ->
            HomeItem(
                id = index + 1L,
                title = generator.generateTitle(),
            )
        }
    }
}

private const val HOME_CATALOG_SIZE = 18

/**
 * Builds the detail screen's content for [contentId].
 *
 * The ID seeds the generator rather than a fixed constant: the content is still "randomized
 * content, predetermined seed" per address, so a demo screenshot and a re-push of the same link
 * always show the same item while a different link shows a different one.
 *
 * For an ID produced by a content surface, the detail's title is that surface's own item name
 * (`SampleItem.name`, `HomeItem.title`, or `SearchResult.name`), so tapping a row opens a detail
 * that matches it. The hero artwork is derived from the same ID in
 * [net.sigmabeta.sage.sample.models.contentHeroImageSeed].
 */
private fun demoContentDetail(contentId: ContentId): ContentDetail? {
    if (!isKnownContentId(contentId)) return null

    val random = Random(contentIdSeed(contentId))
    val generator = StringGenerator(random)
    val title = contentTitleFor(contentId) ?: generator.generateTitle()

    return ContentDetail(
        id = contentId,
        title = title,
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
            val relatedId = index + 1L
            RelatedContent(
                id = relatedId,
                // Seed the row from its own ID so opening it reproduces the same name.
                name = generatedTitleFor(relatedContentId(contentId, relatedId)),
            )
        },
        items = List(ITEM_COUNT) { index ->
            val itemId = index + 1L
            val itemGenerator = StringGenerator(
                Random(contentIdSeed(contentItemContentId(contentId, itemId))),
            )
            ContentItem(
                id = itemId,
                name = itemGenerator.generateTitle(),
                caption = itemGenerator.generateName(),
            )
        },
    )
}

/**
 * The name shown by the surface that produced [contentId], or `null` when the ID is not backed by
 * one of the sample's generated rows (Browse's default, a malformed ID, or a stale link).
 */
private fun contentTitleFor(contentId: ContentId): String? {
    val value = contentId.value
    return when {
        value.startsWith("list-items-with-images:") -> sampleItemName(value)

        value.startsWith("list-items:") -> sampleItemName(value)

        value.startsWith("grid-of-images:") -> sampleItemName(value)

        value.startsWith("home:") -> homeItemName(value)

        value.startsWith("search:") -> searchResultName(value)

        value.startsWith("content-detail:related:") ||
            value.startsWith("content-detail:item:") -> generatedTitleFor(contentId)

        else -> null
    }
}

/** The `SampleItem.name` for `<namespace>:<id>`; both list screens and the grid share the catalog. */
private fun sampleItemName(value: String): String? {
    val itemId = value.substringAfter(':').toLongOrNull()
        ?.takeIf { it in 1..GRID_OF_IMAGES_ITEM_COUNT.toLong() }
        ?: return null

    return demoSampleItems(itemId.toInt()).lastOrNull()?.name
}

/** The `HomeItem.title` for `home:<module-id>:<item-id>`. */
private fun homeItemName(value: String): String? {
    val parts = value.removePrefix("home:").split(':')
    if (parts.size < 2) return null

    val itemId = parts.last().toLongOrNull()?.takeIf { it > 0 } ?: return null
    val moduleId = parts.dropLast(1).joinToString(":")
    if (moduleId.isEmpty()) return null

    return DemoContent.homeItem(seed = moduleId, index = itemId.toInt() - 1)?.title
}

/** The `SearchResult.name` for `search:<list|grid>:<seed>:<result-id>`. */
private fun searchResultName(value: String): String? {
    val parts = value.split(':')
    if (parts.size != 4 || parts[1] !in SEARCH_ROUTE_SEGMENTS) return null

    val seed = parts[2].toIntOrNull() ?: return null
    val resultId = parts[3].toLongOrNull() ?: return null

    return searchResultsForSeedImpl(seed).firstOrNull { it.id == resultId }?.name
}

private fun generatedTitleFor(contentId: ContentId): String =
    StringGenerator(Random(contentIdSeed(contentId))).generateTitle()

private val SEARCH_ROUTE_SEGMENTS = setOf("list", "grid")

/**
 * A stable seed derived from the ID's text.
 *
 * `String.hashCode()` is only 32 bits; this local 64-bit mixing keeps two nearby IDs
 * (`list-items:7`, `list-items:8`) from landing on similar generated content while staying
 * deterministic across runs and platforms.
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

/**
 * Builds generated demo rows for the List Items screens.
 *
 * Every call starts from the same [DEMO_ITEM_SEED], so a screen's content is random-looking but
 * identical on every visit. Screens that need more rows than another extend the prefix rather
 * than re-seeding.
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
 * Returns the `[offset, offset + limit)` window of a deterministic set of [total] rows.
 *
 * Offsets at or past [total] are the catalogue's end and return an empty page, which is how the
 * paginated screens stop asking for more.
 */
internal fun demoSampleItemsWindow(
    offset: Int,
    limit: Int,
    total: Int,
): List<SampleItem> {
    val start = offset.coerceAtLeast(0)
    if (start >= total || limit <= 0) return emptyList()

    val end = (start.toLong() + limit).coerceAtMost(total.toLong()).toInt()
    return demoSampleItems(end).subList(start, end)
}

/**
 * Three to five lorem words, using [generator] and the same [random] sequence as the names.
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

/**
 * Builds the sample's fake results for one repository query.
 *
 * The query and [SearchResultKind] together are the seed: trim the query, lowercase it, and feed
 * the pair's hash into [Random]. Each kind therefore gets its own random count and contents,
 * while a given query + kind always reproduces the same rows.
 */
internal fun searchResultsFor(query: String, kind: SearchResultKind): List<SearchResult> {
    val normalized = normalizeSearchQuery(query)
    if (normalized.length < MIN_SEARCH_QUERY_LENGTH) {
        return emptyList()
    }

    return searchResultsForSeedImpl(searchSeedFor(normalized, kind))
}

/**
 * The rows for a [searchSeedFor] seed.
 *
 * Search routes carry the seed rather than the query text, so a detail can reproduce the exact
 * result set a row came from without re-running the search box.
 */
private fun searchResultsForSeedImpl(seed: Int): List<SearchResult> {
    val random = Random(seed.toLong())
    val generator = StringGenerator(random)
    val resultCount = MIN_SEARCH_RESULT_COUNT +
        random.nextInt(MAX_SEARCH_RESULT_COUNT - MIN_SEARCH_RESULT_COUNT + 1)

    return List(resultCount) { index ->
        SearchResult(
            id = index + 1L,
            name = generator.generateTitle(),
            caption = generator.generateName(),
        )
    }
}

package net.sigmabeta.sage.sample.models

/**
 * The characters a [ContentId] may contain: letters, digits, `_`, `.`, `:` and `-`.
 *
 * Declared before any top-level [ContentId] instance so the value class's `init` check always sees
 * an initialized set, regardless of which declaration runs first.
 */
private val CONTENT_ID_ALLOWED_CHARS =
    ('A'..'Z') + ('a'..'z') + ('0'..'9') + listOf('_', '.', ':', '-')

private const val CONTENT_ID_ALLOWED_DESCRIPTION = "A-Za-z0-9_.:-"

/**
 * A stable content ID.
 *
 * Content detail is a parameterized destination: the app pushes it with an ID, and the ID decides
 * both the generated content and the screen's identity. The value is the complete ID, including an
 * optional namespace (`list-items:7`, `home:big_image_1:5`), because [contentDetailRoute] can only
 * carry one string through `SageEvent.NavigateTo`.
 *
 * The constructor is the validation seam: a [ContentId] always holds a value that can travel in a
 * route. Callers that parse untrusted text use [contentIdOrNull].
 */
// A plain data class rather than a value class: `@JvmInline` is JVM-only, and the browser client
// compiles this same contract. The constructor validation is the part that matters.
data class ContentId(val value: String) {
    init {
        require(isValidContentId(value)) {
            "Content id must match [$CONTENT_ID_ALLOWED_DESCRIPTION]: \"$value\""
        }
    }
}

/**
 * The app route that opens Content detail for [id].
 *
 * This lives here, next to the destination's data contract, so every feature that emits the route
 * (Browse, List Items, List Items with Images, and later the home and search surfaces) shares one
 * encoding instead of inventing its own.
 */
fun contentDetailRoute(id: ContentId): String = "$CONTENT_DETAIL_ROUTE/${id.value}"

/**
 * Decodes a Content detail route into its ID, or `null` when the route has no `content-detail/`
 * prefix or the remainder is missing/invalid.
 *
 * A `null` ID is a real destination state (the screen renders not-found), not an error: the app
 * still navigates for every route under the prefix, so a malformed link opens the explicit
 * not-found state rather than silently doing nothing.
 */
fun contentDetailIdFromRoute(route: String): ContentId? {
    val prefix = "$CONTENT_DETAIL_ROUTE/"
    if (!route.startsWith(prefix)) return null

    return contentIdOrNull(route.removePrefix(prefix))
}

/** Parses [value] into a [ContentId], or `null` when it is blank or outside the contract. */
fun contentIdOrNull(value: String): ContentId? =
    if (isValidContentId(value)) ContentId(value) else null

/**
 * The ID of related-content row [relatedId] shown by the detail [parentId].
 *
 * The parent is embedded, not implied: tapping a related row opens a detail whose name and artwork
 * must match the row that produced it, and a route carries only this one ID.
 */
fun relatedContentId(parentId: ContentId, relatedId: Long): ContentId =
    ContentId("content-detail:related:${parentId.value}:$relatedId")

/** The ID of content-part row [itemId] shown by the detail [parentId]. */
fun contentItemContentId(parentId: ContentId, itemId: Long): ContentId =
    ContentId("content-detail:item:${parentId.value}:$itemId")

/**
 * The `generated://` artwork seed for [contentId]'s detail hero.
 *
 * A detail opened from a row uses the same seed as that row, so the hero shows the same artwork
 * (at a different resolution) as the thumbnail the user tapped. Detail-owned rows are seeded by
 * their parent as well, keeping equal numeric row ids in different details from sharing artwork.
 */
fun contentHeroImageSeed(contentId: ContentId): String {
    contentDetailRow(contentId)?.let { row ->
        return when (row.kind) {
            ContentDetailRowKind.RELATED -> contentDetailRelatedImageSeed(row.parentId, row.rowId)
            ContentDetailRowKind.ITEM -> contentDetailItemImageSeed(row.parentId, row.rowId)
        }
    }

    val value = contentId.value
    return when {
        value.startsWith("list-items-with-images:") ->
            "list-items-with-images/${value.substringAfter(':')}"

        value.startsWith("list-items:") -> "list-items/${value.substringAfter(':')}"

        value.startsWith("grid-of-images:") -> "grid-of-images/${value.substringAfter(':')}"

        value.startsWith("home:") -> homeImageSeed(value) ?: fallbackHeroSeed(value)

        value.startsWith("search:") -> searchImageSeed(value) ?: fallbackHeroSeed(value)

        value.startsWith("browse:") -> "browse/${value.substringAfter(':')}"

        else -> fallbackHeroSeed(value)
    }
}

/** The artwork seed for related row [relatedId] of [parentId]; the row's own hero reuses it. */
fun contentDetailRelatedImageSeed(parentId: ContentId, relatedId: Long): String =
    "content-detail/related/${parentId.value}/$relatedId"

/** The artwork seed for content-part row [itemId] of [parentId]; the row's own hero reuses it. */
fun contentDetailItemImageSeed(parentId: ContentId, itemId: Long): String =
    "content-detail/item/${parentId.value}/$itemId"

private enum class ContentDetailRowKind(val segment: String) {
    RELATED("related"),
    ITEM("item"),
}

private data class ContentDetailRow(
    val parentId: ContentId,
    val kind: ContentDetailRowKind,
    val rowId: Long,
)

/**
 * Parses `content-detail:<kind>:<parent-id>:<row-id>` from the right: the parent is itself a
 * content ID and may contain colons, so only the last separator before the row id is structural.
 */
private fun contentDetailRow(contentId: ContentId): ContentDetailRow? {
    val value = contentId.value
    val prefix = "content-detail:"
    if (!value.startsWith(prefix)) return null

    val remainder = value.removePrefix(prefix)
    val kind = ContentDetailRowKind.entries.firstOrNull {
        remainder.startsWith("${it.segment}:")
    } ?: return null

    val parentAndRow = remainder.removePrefix("${kind.segment}:")
    val rowId = parentAndRow.substringAfterLast(':', missingDelimiterValue = "")
        .toLongOrNull()
        ?.takeIf { it > 0 }
        ?: return null
    val parentValue = parentAndRow.substringBeforeLast(':', missingDelimiterValue = "")
    if (parentValue.isEmpty()) return null

    return ContentDetailRow(
        parentId = contentIdOrNull(parentValue) ?: return null,
        kind = kind,
        rowId = rowId,
    )
}

private fun homeImageSeed(value: String): String? {
    val parts = value.removePrefix("home:").split(':')
    if (parts.size < 2) return null

    val itemId = parts.last().toLongOrNull()?.takeIf { it > 0 } ?: return null
    val moduleId = parts.dropLast(1).joinToString(":")
    if (moduleId.isEmpty()) return null

    return "home/$moduleId/$itemId"
}

private fun searchImageSeed(value: String): String? {
    val parts = value.split(':')
    if (parts.size != 4 || parts[1] !in SEARCH_ROUTE_SEGMENTS) return null

    val seed = parts[2].toIntOrNull() ?: return null
    val resultId = parts[3].toLongOrNull()?.takeIf { it > 0 } ?: return null

    return "search/$seed/$resultId"
}

private fun fallbackHeroSeed(value: String): String = "content-detail/hero/$value"

private val SEARCH_ROUTE_SEGMENTS = setOf("list", "grid")

/**
 * Whether [id] names a content surface the sample knows how to generate.
 *
 * Recognized IDs are namespaced by the surface that produced them (`list-items:7`,
 * `search:list:…`). The repository turns anything else into its explicit not-found result, so a
 * stale or hand-typed link has a defined state rather than generated filler.
 */
fun isKnownContentId(id: ContentId): Boolean = CONTENT_ID_NAMESPACES.any { namespace ->
    id.value == namespace || id.value.startsWith("$namespace:")
}

/**
 * The item Browse's Content detail shortcut opens.
 *
 * A named default instead of a magic string: Browse needs *a* detail, and the route still has to
 * carry a stable ID like every other surface.
 */
val DEFAULT_CONTENT_ID = ContentId("browse:featured")

/** The route prefix shared with `SampleDestination.CONTENT_DETAIL.destName`. */
const val CONTENT_DETAIL_ROUTE = "content-detail"

private fun isValidContentId(value: String): Boolean =
    value.isNotEmpty() && value.all { it in CONTENT_ID_ALLOWED_CHARS }

/** Surfaces whose IDs the demo repository can generate content for. */
private val CONTENT_ID_NAMESPACES = setOf(
    "home",
    "browse",
    "search",
    "list-items",
    "list-items-with-images",
    "grid-of-images",
    "content-detail",
)

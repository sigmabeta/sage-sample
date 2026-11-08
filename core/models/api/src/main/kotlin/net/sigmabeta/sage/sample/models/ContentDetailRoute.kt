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
@JvmInline
value class ContentId(val value: String) {
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

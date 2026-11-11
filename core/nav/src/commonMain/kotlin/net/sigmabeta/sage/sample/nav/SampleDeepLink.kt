package net.sigmabeta.sage.sample.nav

/**
 * Parses a native deep link into the same [SampleRoute] the browser uses.
 *
 * The app owns one custom scheme, [SCHEME], and the URI's authority is its first route segment, so
 * a link mirrors the web path:
 *
 * - `sagesample://browse/list-items` → `/browse/list-items`
 * - `sagesample://settings` → `/settings` (canonicalised to `/home/settings`)
 * - `sagesample://home/content-detail/list-items:7` → `/home/content-detail/list-items:7`
 * - `sagesample://` → Home
 *
 * A bare path (`/browse/list-items` or `browse/list-items`) is accepted too, because that is what a
 * desktop launch argument carries. Any other scheme, and any value that does not name an app
 * destination, decodes to `null`; the host ignores it rather than showing an error.
 *
 * The Android intent filter matches the scheme alone, so the parser is what decides whether a URI
 * is an app link. Keeping that decision here — next to [SampleRouteUrl] — is what stops the
 * Android and desktop hosts from growing two slightly different link formats.
 */
object SampleDeepLink {
    /** The custom scheme the Android manifest claims and desktop launch arguments may carry. */
    const val SCHEME = "sagesample"

    private const val SCHEME_PREFIX = "$SCHEME://"

    /** Decodes [value] to a route, or null when it is not an app deep link. */
    fun routeFor(value: String): SampleRoute? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null

        val path = when {
            trimmed.startsWith(SCHEME_PREFIX, ignoreCase = true) ->
                "/" + trimmed.substring(SCHEME_PREFIX.length)

            trimmed.startsWith("/") -> trimmed

            // Anything else carrying a scheme belongs to another app.
            trimmed.contains("://") -> return null

            else -> "/$trimmed"
        }
        return SampleRouteUrl.decode(path)
    }
}

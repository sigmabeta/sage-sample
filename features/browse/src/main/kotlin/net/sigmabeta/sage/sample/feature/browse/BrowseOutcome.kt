package net.sigmabeta.sage.sample.feature.browse

/**
 * The three demo outcomes Browse can load.
 *
 * The sample exposes them through [BrowseAction] rows so a user can walk from content into the
 * empty and error presentations without a real backend.
 */
enum class BrowseOutcome {
    SUCCESS,
    EMPTY,
    ERROR,
}

package net.sigmabeta.sage.sample.feature.browse

/**
 * The three demo outcomes Browse can load.
 *
 * The sample exposes them through [BrowseAction] rows so a user can walk from content into the
 * empty and error presentations without a real backend. [CONTENT] matches
 * [net.sigmabeta.sage.appcomm.LCE.Content] and [BrowseAction.LoadContent].
 */
enum class BrowseOutcome {
    CONTENT,
    EMPTY,
    ERROR,
}

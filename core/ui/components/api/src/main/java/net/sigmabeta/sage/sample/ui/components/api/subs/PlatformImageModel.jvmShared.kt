package net.sigmabeta.sage.sample.ui.components.api.subs

import okio.Path.Companion.toPath

/**
 * Shared by Android and desktop/JVM: rewrite an absolute local filesystem path into an okio
 * [okio.Path] so Coil loads it via [coil3.map.PathMapper] (literal path, no URI parsing) instead
 * of [coil3.map.StringMapper] (which treats `#`/`?` in the path as URI syntax and truncates it).
 *
 * URLs and SAF `content://` URIs aren't absolute paths, so they pass through untouched for Coil's
 * own mappers.
 */
internal actual fun platformImageModel(info: Any?): Any? =
    if (info is String && info.isAbsoluteLocalPath()) info.toPath() else info

private fun String.isAbsoluteLocalPath(): Boolean {
    if ("://" in this) return false
    return startsWith('/') || WINDOWS_ABSOLUTE_PATH.matches(this)
}

private val WINDOWS_ABSOLUTE_PATH = Regex("""^[A-Za-z]:[\\/].*""")

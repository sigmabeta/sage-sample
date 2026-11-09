package net.sigmabeta.sage.sample.ui.components.api.subs

/**
 * The browser has no absolute filesystem paths to normalise: every image reference is a URL (or
 * a `generated://` seed), so the Coil model passes through untouched.
 */
internal actual fun platformImageModel(info: Any?): Any? = info

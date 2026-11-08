package net.sigmabeta.sage.sample.nav

import net.sigmabeta.sage.sample.models.ContentId

/**
 * The Voyager screen key for Content detail.
 *
 * The full ID is embedded so Voyager's per-screen ViewModelStore (and therefore the view model) is
 * distinct per ID. A missing or unusable ID is a real not-found state with its own key, not a
 * reason to reuse another item's screen.
 */
fun contentDetailScreenKey(contentId: ContentId?): String =
    "ContentDetail:${contentId?.value ?: CONTENT_DETAIL_NOT_FOUND_KEY}"

private const val CONTENT_DETAIL_NOT_FOUND_KEY = "none"

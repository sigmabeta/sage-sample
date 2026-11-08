package net.sigmabeta.sage.sample.models

import net.sigmabeta.sage.ui.Icon

/**
 * One generated content item and everything its detail screen renders.
 *
 * The shape follows a detail screen — the item itself (title and hero art), a set of descriptive
 * fields, related items, parts, and call-to-action rows — but every label and value is generated
 * from a predetermined seed rather than a fixed vocabulary. The sample has no library backend, so
 * [DemoContentDetailRepository] builds it.
 */
data class ContentDetail(
    val id: ContentId,
    val title: String,
    val details: List<ContentDetailField>,
    val actions: List<ContentAction>,
    val related: List<RelatedContent>,
    val items: List<ContentItem>,
)

/** One generated label/value row in the details block. */
data class ContentDetailField(
    val label: String,
    val value: String,
)

/** One generated call-to-action row: its text and icon are both drawn from the seed. */
data class ContentAction(
    val label: String,
    val icon: Icon,
)

/** One related content item, shown in the detail screen's horizontal scroller. */
data class RelatedContent(
    val id: Long,
    val name: String,
)

/** One generated part of the content item, shown as an image/name/caption row. */
data class ContentItem(
    val id: Long,
    val name: String,
    val caption: String,
)

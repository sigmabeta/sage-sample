package net.sigmabeta.sage.sample.feature.contentdetail

import net.sigmabeta.sage.ui.SageStringId

/**
 * Every string the Content detail feature renders.
 *
 * As with [net.sigmabeta.sage.sample.feature.home.HomeStringId], the app module owns the
 * id → `R.string` mapping; this module never touches Android resources. Content text itself —
 * titles, labels, CTA text, names and captions — is generated demo content and lives in the
 * repository, like Home's generated rows.
 */
enum class ContentDetailStringId : SageStringId {
    CONTENT_DETAIL_TITLE,
    CONTENT_DETAIL_SECTION_DETAILS,
    CONTENT_DETAIL_SECTION_RELATED,
    CONTENT_DETAIL_SECTION_ITEMS,
    CONTENT_DETAIL_ITEM_CLICKED,
    CONTENT_DETAIL_NOT_FOUND,
}

package net.sigmabeta.sage.sample.feature.contentdetail
import net.sigmabeta.sage.sample.models.ContentId
import net.sigmabeta.sage.sample.models.ContentItem
import net.sigmabeta.sage.sample.models.RelatedContent

import net.sigmabeta.sage.appcomm.SageAction

/**
 * Actions the Content detail screen's rows carry.
 *
 * A feature owns its vocabulary: the app forwards a tap through [SageAction], and
 * [ContentDetailViewModel] decides what it means. Related rows and content parts are
 * detail-capable, so they open another detail; the hero and generated call-to-action rows have no
 * destination and report a snackbar instead.
 */
sealed class ContentDetailAction : SageAction() {
    data object HeroClicked : ContentDetailAction()
    data class CtaClicked(val label: String) : ContentDetailAction()
    data class ItemClicked(val item: ContentItem) : ContentDetailAction()
    data class RelatedClicked(val related: RelatedContent) : ContentDetailAction()
}

/** The stable content ID for the related row [relatedId]. */
internal fun relatedContentId(relatedId: Long): ContentId =
    ContentId("content-detail:related:$relatedId")

/** The stable content ID for content part [itemId]. */
internal fun contentItemContentId(itemId: Long): ContentId =
    ContentId("content-detail:item:$itemId")

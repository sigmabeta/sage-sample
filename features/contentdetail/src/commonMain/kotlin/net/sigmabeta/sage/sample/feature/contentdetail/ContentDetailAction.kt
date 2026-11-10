package net.sigmabeta.sage.sample.feature.contentdetail

import net.sigmabeta.sage.sample.models.ContentId

import net.sigmabeta.sage.appcomm.SageAction

/**
 * Actions the Content detail screen's rows carry.
 *
 * A feature owns its vocabulary: the app forwards a tap through [SageAction], and
 * [ContentDetailViewModel] decides what it means. Related rows and content parts are
 * detail-capable, so they open another detail; the hero and generated call-to-action rows have no
 * destination and report a snackbar instead.
 *
 * [ItemClicked.contentId] and [RelatedClicked.contentId] already include the current detail as the
 * parent, so a row opens the exact item that produced it rather than a same-numbered row from a
 * different detail.
 */
sealed class ContentDetailAction : SageAction() {
    data object HeroClicked : ContentDetailAction()
    data class CtaClicked(val label: String) : ContentDetailAction()
    data class ItemClicked(val contentId: ContentId) : ContentDetailAction()
    data class RelatedClicked(val contentId: ContentId) : ContentDetailAction()
}

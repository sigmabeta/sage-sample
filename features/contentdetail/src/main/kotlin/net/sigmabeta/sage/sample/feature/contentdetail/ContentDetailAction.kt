package net.sigmabeta.sage.sample.feature.contentdetail

import net.sigmabeta.sage.appcomm.SageAction

/**
 * Actions the Content detail screen's rows carry.
 *
 * A feature owns its vocabulary: the app forwards a tap through [SageAction], and
 * [ContentDetailViewModel] decides what it means. The generated actions have no backend
 * behaviour, so every tap names its row in a snackbar.
 */
sealed class ContentDetailAction : SageAction() {
    data object HeroClicked : ContentDetailAction()
    data class CtaClicked(val label: String) : ContentDetailAction()
    data class ItemClicked(val item: ContentItem) : ContentDetailAction()
    data class RelatedClicked(val related: RelatedContent) : ContentDetailAction()
}

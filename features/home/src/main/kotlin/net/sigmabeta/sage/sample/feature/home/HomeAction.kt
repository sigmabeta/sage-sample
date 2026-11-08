package net.sigmabeta.sage.sample.feature.home
import net.sigmabeta.sage.sample.models.ContentId

import net.sigmabeta.sage.appcomm.SageAction

/**
 * Actions Home rows carry.
 *
 * A feature owns its vocabulary: a module builds its rows with one of these, and
 * [HomeViewModel] decides what it means. These are not init actions, so each one is logged as a
 * user action rather than a screen view.
 */
sealed class HomeAction : SageAction() {
    /** A module's image was tapped; [contentId] names the detail screen it opens. */
    data class ItemClicked(val contentId: ContentId) : HomeAction()
}

/**
 * The stable content ID for [itemId] shown by the module instance [moduleId].
 *
 * Home repeats module types, so the same numeric item can appear in more than one row. The
 * module id (already unique within a plan) is part of the namespace, keeping the routes distinct
 * while still reproducing the same detail for the same row.
 */
internal fun homeContentId(moduleId: String, itemId: Long): ContentId =
    ContentId("home:$moduleId:$itemId")

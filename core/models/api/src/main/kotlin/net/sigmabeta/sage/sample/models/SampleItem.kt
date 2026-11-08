package net.sigmabeta.sage.sample.models

/**
 * One generated row in the List Items screens.
 *
 * [id] is the row's stable list identity. SAGE's `ListScreen` keys its `LazyColumn` by
 * `ListModel.dataId`, and both List Items states map this id rather than a row position, so a
 * reload keeps keyed item identity (and the animation/scroll state tied to it) stable.
 */
data class SampleItem(
    val id: Long,
    val name: String,
    val caption: String,
)

package net.sigmabeta.sage.sample.feature.search

/**
 * One generated search hit.
 *
 * Results are domain data, not list models: [SearchState] maps the list query's results onto
 * `ImageNameCaptionListModel` rows and the grid query's results onto `GridImageListModel` cells,
 * keeping the feature independent of the renderer.
 *
 * [id] is only unique within one result set. It is the row's stable list identity and also
 * feeds the deterministic `generated://` artwork URL, so the same query always renders the same
 * rows and images.
 */
data class SearchResult(
    val id: Long,
    val name: String,
    val caption: String,
)

package net.sigmabeta.sage.sample.models

/**
 * One demo item the Home modules can surface.
 *
 * The sample has no catalog backend; [DemoHomeRepository] seeds [id] and [title] so a given index
 * always produces the same item. Artwork is derived from [id], so the same item looks consistent
 * wherever it appears.
 */
data class HomeItem(
    val id: Long,
    val title: String,
)

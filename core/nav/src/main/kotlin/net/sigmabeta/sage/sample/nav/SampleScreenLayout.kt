package net.sigmabeta.sage.sample.nav

import net.sigmabeta.sage.list.ColumnType
import net.sigmabeta.sage.list.WidthClass

/**
 * How the app renders a screen's items.
 *
 * [List] is SAGE's single-column `ListScreen`, the presentation every current screen keeps. [Grid]
 * is SAGE's `GridScreen`, including its staggered variant.
 */
sealed interface SampleScreenLayout {
    data object List : SampleScreenLayout

    data class Grid(
        val columns: Int,
        val staggered: Boolean,
        val allowHorizScroller: Boolean,
    ) : SampleScreenLayout
}

/**
 * Chooses between the list and grid presentations from the screen's SAGE [ColumnType] and the
 * window's [WidthClass].
 *
 * At most one column always means [SampleScreenLayout.List], so a screen that does not opt into
 * columns keeps exactly its existing presentation. That includes zero columns: SAGE's
 * `numberOfColumns` is integer division, so a wide `Regular`/`Staggered` descriptor on a narrow
 * window yields 0, which is the same single-column case rather than an error.
 */
fun screenLayoutFor(columnType: ColumnType, widthClass: WidthClass): SampleScreenLayout {
    val columns = columnType.numberOfColumns(widthClass)

    return if (columns <= 1) {
        SampleScreenLayout.List
    } else {
        SampleScreenLayout.Grid(
            columns = columns,
            staggered = columnType is ColumnType.Staggered,
            allowHorizScroller = (columnType as? ColumnType.Staggered)?.allowHorizScroller ?: false,
        )
    }
}

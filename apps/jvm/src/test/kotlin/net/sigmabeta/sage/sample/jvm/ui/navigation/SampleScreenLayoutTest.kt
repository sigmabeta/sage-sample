package net.sigmabeta.sage.sample.jvm.ui.navigation

import net.sigmabeta.sage.list.ColumnType
import net.sigmabeta.sage.list.WidthClass
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Layout selection turns a screen's SAGE [ColumnType] plus the window width class into either the
 * existing single-column [SampleScreenLayout.List] or SAGE's grid presentation.
 *
 * The running sample keeps every screen at [ColumnType.One]; these tests are what exercise the
 * multi-column path.
 */
class SampleScreenLayoutTest {
    @Test
    fun `one column stays a list at every width class`() {
        WidthClass.entries.forEach { widthClass ->
            assertEquals(
                "ColumnType.One must keep the existing ListScreen at $widthClass",
                SampleScreenLayout.List,
                screenLayoutFor(ColumnType.One, widthClass),
            )
        }
    }

    @Test
    fun `a regular column type becomes a list when only one column fits`() {
        assertEquals(SampleScreenLayout.List, screenLayoutFor(ColumnType.Regular(240), WidthClass.COMPACT))
    }

    @Test
    fun `a regular column type becomes a grid at medium width`() {
        assertEquals(
            SampleScreenLayout.Grid(columns = 3, staggered = false, allowHorizScroller = false),
            screenLayoutFor(ColumnType.Regular(240), WidthClass.MEDIUM),
        )
    }

    @Test
    fun `a regular column type becomes a grid at expanded width`() {
        assertEquals(
            SampleScreenLayout.Grid(columns = 3, staggered = false, allowHorizScroller = false),
            screenLayoutFor(ColumnType.Regular(240), WidthClass.EXPANDED),
        )
    }

    @Test
    fun `a narrow regular column type grids on a compact window`() {
        assertEquals(
            SampleScreenLayout.Grid(columns = 3, staggered = false, allowHorizScroller = false),
            screenLayoutFor(ColumnType.Regular(100), WidthClass.COMPACT),
        )
    }

    @Test
    fun `a column type that fits at most one column stays a list`() {
        ColumnType.Regular(500).let { columnType ->
            WidthClass.entries.forEach { widthClass ->
                assertEquals(
                    "Regular(500) fits at most one column at $widthClass, so it must stay List",
                    SampleScreenLayout.List,
                    screenLayoutFor(columnType, widthClass),
                )
            }
        }
    }

    @Test
    fun `two columns fit at medium width`() {
        assertEquals(
            SampleScreenLayout.Grid(columns = 2, staggered = false, allowHorizScroller = false),
            screenLayoutFor(ColumnType.Regular(300), WidthClass.MEDIUM),
        )
    }

    @Test
    fun `a staggered column type keeps its staggered and horizontal-scroller flags`() {
        assertEquals(
            SampleScreenLayout.Grid(columns = 3, staggered = true, allowHorizScroller = true),
            screenLayoutFor(ColumnType.Staggered(widthInDp = 240, allowHorizScroller = true), WidthClass.MEDIUM),
        )
    }

    @Test
    fun `a staggered column type passes allowHorizScroller false through`() {
        assertEquals(
            SampleScreenLayout.Grid(columns = 3, staggered = true, allowHorizScroller = false),
            screenLayoutFor(ColumnType.Staggered(widthInDp = 100, allowHorizScroller = false), WidthClass.COMPACT),
        )
    }

    @Test
    fun `a staggered column type with only one column stays a list`() {
        assertEquals(
            SampleScreenLayout.List,
            screenLayoutFor(ColumnType.Staggered(widthInDp = 240, allowHorizScroller = true), WidthClass.COMPACT),
        )
    }

    @Test
    fun `width classes switch at the 600 and 840 dp breakpoints`() {
        assertEquals(WidthClass.COMPACT, widthClassFromDp(0))
        assertEquals(WidthClass.COMPACT, widthClassFromDp(599))
        assertEquals(WidthClass.MEDIUM, widthClassFromDp(600))
        assertEquals(WidthClass.MEDIUM, widthClassFromDp(839))
        assertEquals(WidthClass.EXPANDED, widthClassFromDp(840))
        assertEquals(WidthClass.EXPANDED, widthClassFromDp(2_000))
    }
}

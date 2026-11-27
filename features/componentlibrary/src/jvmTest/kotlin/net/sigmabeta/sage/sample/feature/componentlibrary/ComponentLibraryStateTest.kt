package net.sigmabeta.sage.sample.feature.componentlibrary

import net.sigmabeta.sage.components.ListModel
import net.sigmabeta.sage.list.ColumnType
import net.sigmabeta.sage.sample.strings.api.SampleStringId
import net.sigmabeta.sage.sample.ui.components.models.ButtonListModel
import net.sigmabeta.sage.sample.ui.components.models.FormMessageListModel
import net.sigmabeta.sage.sample.ui.components.models.FormMessageTone
import net.sigmabeta.sage.sample.ui.components.models.TextFieldListModel
import net.sigmabeta.sage.ui.componentlibrary.ComponentLibraryConfig
import net.sigmabeta.sage.ui.componentlibrary.ComponentLibraryLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ComponentLibraryStateTest {
    @Test
    fun `column type follows the layout`() {
        ComponentLibraryLayout.entries.forEach { layout ->
            val state = ComponentLibraryState(ComponentLibraryConfig(layout = layout))
            assertEquals(layout.columnType, state.columnType)
        }
        assertEquals(ColumnType.One, ComponentLibraryState().columnType)
    }

    @Test
    fun `the title is the library name with back shown`() {
        val title = ComponentLibraryState().title(FakeStringProvider)

        assertEquals(SampleStringId.NAV_COMPONENT_LIBRARY.toString(), title.title)
        assertTrue(title.shouldShowBack)
    }

    @Test
    fun `the sample form rows follow the catalog in every layout`() {
        ComponentLibraryLayout.entries.forEach { layout ->
            val items = ComponentLibraryState(ComponentLibraryConfig(layout = layout))
                .toListItems(FakeStringProvider)

            assertEquals(1, items.filterIsInstance<TextFieldListModel>().size)
            assertEquals(1, items.filterIsInstance<ButtonListModel>().size)
            val messages = items.filterIsInstance<FormMessageListModel>()
            assertEquals(
                setOf(FormMessageTone.ERROR, FormMessageTone.INFO),
                messages.map { it.tone }.toSet(),
            )
        }
    }

    @Test
    fun `no two rows share a dataId, extras included`() {
        ComponentLibraryLayout.entries.forEach { layout ->
            val items: List<ListModel> = ComponentLibraryState(ComponentLibraryConfig(layout = layout))
                .toListItems(FakeStringProvider)
            val ids = items.map { it.dataId }

            assertEquals("duplicate dataIds in $layout", ids.size, ids.toSet().size)
        }
    }
}

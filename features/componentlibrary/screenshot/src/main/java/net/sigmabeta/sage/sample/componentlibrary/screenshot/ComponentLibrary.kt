package net.sigmabeta.sage.sample.componentlibrary.screenshot

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import net.sigmabeta.sage.list.WidthClass
import net.sigmabeta.sage.sample.feature.componentlibrary.ComponentLibraryState
import net.sigmabeta.sage.sample.ui.previews.DevicePreviews
import net.sigmabeta.sage.sample.ui.previews.ListScreenPreview
import net.sigmabeta.sage.sample.ui.previews.previewWidthClass
import net.sigmabeta.sage.ui.componentlibrary.ComponentLibraryConfig
import net.sigmabeta.sage.ui.componentlibrary.ComponentLibraryHost
import net.sigmabeta.sage.ui.componentlibrary.ComponentLibraryLayout

@DevicePreviews
@Composable
internal fun ComponentLibraryList(
    darkTheme: Boolean = isSystemInDarkTheme(),
    syntheticWidthClass: WidthClass = previewWidthClass(),
) = Fixture(ComponentLibraryLayout.LIST, darkTheme, syntheticWidthClass)

@DevicePreviews
@Composable
internal fun ComponentLibraryGrid(
    darkTheme: Boolean = isSystemInDarkTheme(),
    syntheticWidthClass: WidthClass = previewWidthClass(),
) = Fixture(ComponentLibraryLayout.GRID, darkTheme, syntheticWidthClass)

@DevicePreviews
@Composable
internal fun ComponentLibraryColumns(
    darkTheme: Boolean = isSystemInDarkTheme(),
    syntheticWidthClass: WidthClass = previewWidthClass(),
) = Fixture(ComponentLibraryLayout.COLUMNS, darkTheme, syntheticWidthClass)

/** The default config (default style, the fixed seed-0 content) in one [layout]. */
@Composable
private fun Fixture(layout: ComponentLibraryLayout, darkTheme: Boolean, syntheticWidthClass: WidthClass) {
    val state = ComponentLibraryState(ComponentLibraryConfig(layout = layout))
    ComponentLibraryHost(state.config.style) {
        ListScreenPreview(
            screenState = state,
            darkTheme = darkTheme,
            syntheticWidthClass = syntheticWidthClass,
        )
    }
}

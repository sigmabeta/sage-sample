package net.sigmabeta.sage.sample.listitems.screenshot

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import net.sigmabeta.sage.list.WidthClass
import net.sigmabeta.sage.sample.feature.listitems.ListItemsState
import net.sigmabeta.sage.sample.ui.previews.DevicePreviews
import net.sigmabeta.sage.sample.ui.previews.ListScreenPreview
import net.sigmabeta.sage.sample.ui.previews.previewWidthClass

@DevicePreviews
@Composable
internal fun ListItems(
    darkTheme: Boolean = isSystemInDarkTheme(),
    syntheticWidthClass: WidthClass = previewWidthClass(),
) {
    ListScreenPreview(
        screenState = ListItemsState(content = ListItemsFixtures.loaded),
        darkTheme = darkTheme,
        syntheticWidthClass = syntheticWidthClass,
    )
}

@DevicePreviews
@Composable
internal fun ListItemsLoading(
    darkTheme: Boolean = isSystemInDarkTheme(),
    syntheticWidthClass: WidthClass = previewWidthClass(),
) {
    ListScreenPreview(
        screenState = ListItemsState(content = ListItemsFixtures.loading),
        darkTheme = darkTheme,
        syntheticWidthClass = syntheticWidthClass,
    )
}

@DevicePreviews
@Composable
internal fun ListItemsEmpty(
    darkTheme: Boolean = isSystemInDarkTheme(),
    syntheticWidthClass: WidthClass = previewWidthClass(),
) {
    ListScreenPreview(
        screenState = ListItemsState(content = ListItemsFixtures.empty),
        darkTheme = darkTheme,
        syntheticWidthClass = syntheticWidthClass,
    )
}

@DevicePreviews
@Composable
internal fun ListItemsError(
    darkTheme: Boolean = isSystemInDarkTheme(),
    syntheticWidthClass: WidthClass = previewWidthClass(),
) {
    ListScreenPreview(
        screenState = ListItemsState(content = ListItemsFixtures.error),
        darkTheme = darkTheme,
        syntheticWidthClass = syntheticWidthClass,
    )
}

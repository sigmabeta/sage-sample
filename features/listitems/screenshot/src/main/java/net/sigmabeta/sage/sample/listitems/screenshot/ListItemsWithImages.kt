package net.sigmabeta.sage.sample.listitems.screenshot

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import net.sigmabeta.sage.list.WidthClass
import net.sigmabeta.sage.sample.feature.listitems.ListItemsWithImagesState
import net.sigmabeta.sage.sample.ui.previews.DevicePreviews
import net.sigmabeta.sage.sample.ui.previews.ListScreenPreview
import net.sigmabeta.sage.sample.ui.previews.previewWidthClass

@DevicePreviews
@Composable
internal fun ListItemsWithImages(
    darkTheme: Boolean = isSystemInDarkTheme(),
    syntheticWidthClass: WidthClass = previewWidthClass(),
) {
    ListScreenPreview(
        screenState = ListItemsWithImagesState(content = ListItemsFixtures.loaded),
        darkTheme = darkTheme,
        syntheticWidthClass = syntheticWidthClass,
    )
}

@DevicePreviews
@Composable
internal fun ListItemsWithImagesLoading(
    darkTheme: Boolean = isSystemInDarkTheme(),
    syntheticWidthClass: WidthClass = previewWidthClass(),
) {
    ListScreenPreview(
        screenState = ListItemsWithImagesState(content = ListItemsFixtures.loading),
        darkTheme = darkTheme,
        syntheticWidthClass = syntheticWidthClass,
    )
}

@DevicePreviews
@Composable
internal fun ListItemsWithImagesEmpty(
    darkTheme: Boolean = isSystemInDarkTheme(),
    syntheticWidthClass: WidthClass = previewWidthClass(),
) {
    ListScreenPreview(
        screenState = ListItemsWithImagesState(content = ListItemsFixtures.empty),
        darkTheme = darkTheme,
        syntheticWidthClass = syntheticWidthClass,
    )
}

@DevicePreviews
@Composable
internal fun ListItemsWithImagesError(
    darkTheme: Boolean = isSystemInDarkTheme(),
    syntheticWidthClass: WidthClass = previewWidthClass(),
) {
    ListScreenPreview(
        screenState = ListItemsWithImagesState(content = ListItemsFixtures.error),
        darkTheme = darkTheme,
        syntheticWidthClass = syntheticWidthClass,
    )
}

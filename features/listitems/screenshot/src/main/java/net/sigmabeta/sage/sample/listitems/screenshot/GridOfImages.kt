package net.sigmabeta.sage.sample.listitems.screenshot

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import net.sigmabeta.sage.list.WidthClass
import net.sigmabeta.sage.sample.feature.listitems.GridOfImagesState
import net.sigmabeta.sage.sample.ui.previews.DevicePreviews
import net.sigmabeta.sage.sample.ui.previews.ListScreenPreview
import net.sigmabeta.sage.sample.ui.previews.previewWidthClass

@DevicePreviews
@Composable
internal fun GridOfImages(
    darkTheme: Boolean = isSystemInDarkTheme(),
    syntheticWidthClass: WidthClass = previewWidthClass(),
) {
    ListScreenPreview(
        screenState = GridOfImagesState(content = ListItemsFixtures.loaded),
        darkTheme = darkTheme,
        syntheticWidthClass = syntheticWidthClass,
    )
}

@DevicePreviews
@Composable
internal fun GridOfImagesLoading(
    darkTheme: Boolean = isSystemInDarkTheme(),
    syntheticWidthClass: WidthClass = previewWidthClass(),
) {
    ListScreenPreview(
        screenState = GridOfImagesState(content = ListItemsFixtures.loading),
        darkTheme = darkTheme,
        syntheticWidthClass = syntheticWidthClass,
    )
}

@DevicePreviews
@Composable
internal fun GridOfImagesEmpty(
    darkTheme: Boolean = isSystemInDarkTheme(),
    syntheticWidthClass: WidthClass = previewWidthClass(),
) {
    ListScreenPreview(
        screenState = GridOfImagesState(content = ListItemsFixtures.empty),
        darkTheme = darkTheme,
        syntheticWidthClass = syntheticWidthClass,
    )
}

@DevicePreviews
@Composable
internal fun GridOfImagesError(
    darkTheme: Boolean = isSystemInDarkTheme(),
    syntheticWidthClass: WidthClass = previewWidthClass(),
) {
    ListScreenPreview(
        screenState = GridOfImagesState(content = ListItemsFixtures.error),
        darkTheme = darkTheme,
        syntheticWidthClass = syntheticWidthClass,
    )
}

package net.sigmabeta.sage.sample.browse.screenshot

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import net.sigmabeta.sage.list.WidthClass
import net.sigmabeta.sage.sample.feature.browse.BrowseState
import net.sigmabeta.sage.sample.ui.previews.DevicePreviews
import net.sigmabeta.sage.sample.ui.previews.ListScreenPreview
import net.sigmabeta.sage.sample.ui.previews.previewWidthClass

/**
 * Browse's static destination list. The state is a `data object`, so the fixture needs no
 * builders: it is the screen the app renders.
 */
@DevicePreviews
@Composable
internal fun Browse(
    darkTheme: Boolean = isSystemInDarkTheme(),
    syntheticWidthClass: WidthClass = previewWidthClass(),
) {
    ListScreenPreview(
        screenState = BrowseState,
        darkTheme = darkTheme,
        syntheticWidthClass = syntheticWidthClass,
    )
}

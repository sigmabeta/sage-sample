package net.sigmabeta.sage.sample.contentdetail.screenshot

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.list.WidthClass
import net.sigmabeta.sage.sample.feature.contentdetail.ContentDetailState
import net.sigmabeta.sage.sample.ui.previews.DevicePreviews
import net.sigmabeta.sage.sample.ui.previews.ListScreenPreview
import net.sigmabeta.sage.sample.ui.previews.fake.FakeModelGenerator
import net.sigmabeta.sage.sample.ui.previews.previewWidthClass

@DevicePreviews
@Composable
internal fun ContentDetailLoaded(
    darkTheme: Boolean = isSystemInDarkTheme(),
    syntheticWidthClass: WidthClass = previewWidthClass(),
) {
    ListScreenPreview(
        screenState = ContentDetailState(
            content = LCE.Content(FakeModelGenerator().randomContentDetail()),
        ),
        darkTheme = darkTheme,
        syntheticWidthClass = syntheticWidthClass,
    )
}

@DevicePreviews
@Composable
internal fun ContentDetailLoading(
    darkTheme: Boolean = isSystemInDarkTheme(),
    syntheticWidthClass: WidthClass = previewWidthClass(),
) {
    ListScreenPreview(
        screenState = ContentDetailState(content = LCE.Loading(OPERATION_NAME)),
        darkTheme = darkTheme,
        syntheticWidthClass = syntheticWidthClass,
    )
}

@DevicePreviews
@Composable
internal fun ContentDetailNotFound(
    darkTheme: Boolean = isSystemInDarkTheme(),
    syntheticWidthClass: WidthClass = previewWidthClass(),
) {
    ListScreenPreview(
        screenState = ContentDetailState(content = LCE.Content(null)),
        darkTheme = darkTheme,
        syntheticWidthClass = syntheticWidthClass,
    )
}

@DevicePreviews
@Composable
internal fun ContentDetailError(
    darkTheme: Boolean = isSystemInDarkTheme(),
    syntheticWidthClass: WidthClass = previewWidthClass(),
) {
    ListScreenPreview(
        screenState = ContentDetailState(
            content = LCE.Error(OPERATION_NAME, IllegalStateException("Screenshot fixture")),
        ),
        darkTheme = darkTheme,
        syntheticWidthClass = syntheticWidthClass,
    )
}

private const val OPERATION_NAME = "content-detail"

package net.sigmabeta.sage.sample.search.screenshot

import net.sigmabeta.sage.sample.strings.api.SampleStringId

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import net.sigmabeta.sage.appcomm.ActionSink
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.sample.feature.search.SearchState
import net.sigmabeta.sage.sample.ui.components.api.SearchContent
import net.sigmabeta.sage.sample.ui.previews.DevicePreviews
import net.sigmabeta.sage.sample.ui.previews.ScreenPreview
import net.sigmabeta.sage.sample.ui.previews.fake.FakeModelGenerator

@DevicePreviews
@Composable
internal fun SearchResting(darkTheme: Boolean = isSystemInDarkTheme()) {
    SearchPreview(
        screenState = SearchState(history = HISTORY),
        query = "",
        darkTheme = darkTheme,
    )
}

@DevicePreviews
@Composable
internal fun SearchResults(darkTheme: Boolean = isSystemInDarkTheme()) {
    SearchPreview(
        screenState = SearchState(
            query = SEARCH_QUERY,
            submittedQuery = SEARCH_QUERY,
            listResults = LCE.Content(FakeModelGenerator().randomSearchResults("List result")),
            gridResults = LCE.Content(FakeModelGenerator().randomSearchResults("Grid result")),
        ),
        query = SEARCH_QUERY,
        darkTheme = darkTheme,
    )
}

@DevicePreviews
@Composable
internal fun SearchLoading(darkTheme: Boolean = isSystemInDarkTheme()) {
    SearchPreview(
        screenState = SearchState(
            query = SEARCH_QUERY,
            submittedQuery = SEARCH_QUERY,
            listResults = LCE.Loading("search.list"),
            gridResults = LCE.Loading("search.grid"),
        ),
        query = SEARCH_QUERY,
        darkTheme = darkTheme,
    )
}

@DevicePreviews
@Composable
internal fun SearchNoResults(darkTheme: Boolean = isSystemInDarkTheme()) {
    SearchPreview(
        screenState = SearchState(
            query = NO_RESULTS_QUERY,
            submittedQuery = NO_RESULTS_QUERY,
            listResults = LCE.Content(emptyList()),
            gridResults = LCE.Content(emptyList()),
        ),
        query = NO_RESULTS_QUERY,
        darkTheme = darkTheme,
    )
}

/**
 * Search owns its chrome (the in-screen field replaces the app top bar), so it renders through
 * [ScreenPreview] rather than the generic list pipeline, mirroring `SearchScreen`.
 */
@Composable
private fun SearchPreview(
    screenState: SearchState,
    query: String,
    darkTheme: Boolean,
) {
    ScreenPreview(darkTheme = darkTheme) { provider ->
        val state = screenState.toActual(provider)
        SearchContent(
            listItems = state.listItems,
            query = query,
            hint = provider.getString(SampleStringId.SEARCH_HINT),
            clearContentDescription = provider.getString(SampleStringId.SEARCH_CLEAR_DESCRIPTION),
            backContentDescription = provider.getString(SampleStringId.SEARCH_BACK_DESCRIPTION),
            onQueryChange = {},
            onClear = {},
            onBack = {},
            actionSink = ActionSink { },
        )
    }
}

private val HISTORY = listOf("chiptune", "soundtrack", "pixel art")

private const val SEARCH_QUERY = "sample"
private const val NO_RESULTS_QUERY = "zzzz"

package net.sigmabeta.sage.sample.home.screenshot

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.components.GridImageListModel
import net.sigmabeta.sage.images.SourceInfo
import net.sigmabeta.sage.list.WidthClass
import net.sigmabeta.sage.sample.feature.home.HomeModuleSection
import net.sigmabeta.sage.sample.feature.home.HomeSectionState
import net.sigmabeta.sage.sample.feature.home.HomeState
import net.sigmabeta.sage.sample.models.HomeItem
import net.sigmabeta.sage.sample.strings.api.SampleStringId
import net.sigmabeta.sage.sample.strings.real.SampleStringProvider
import net.sigmabeta.sage.sample.ui.previews.DevicePreviews
import net.sigmabeta.sage.sample.ui.previews.ListScreenPreview
import net.sigmabeta.sage.sample.ui.previews.fake.FakeModelGenerator
import net.sigmabeta.sage.sample.ui.previews.previewWidthClass
import net.sigmabeta.sage.ui.Icon

@DevicePreviews
@Composable
internal fun Home(
    darkTheme: Boolean = isSystemInDarkTheme(),
    syntheticWidthClass: WidthClass = previewWidthClass(),
) {
    ListScreenPreview(
        screenState = homeContentState(),
        darkTheme = darkTheme,
        syntheticWidthClass = syntheticWidthClass,
    )
}

@DevicePreviews
@Composable
internal fun HomeLoading(
    darkTheme: Boolean = isSystemInDarkTheme(),
    syntheticWidthClass: WidthClass = previewWidthClass(),
) {
    ListScreenPreview(
        screenState = homeLoadingState(),
        darkTheme = darkTheme,
        syntheticWidthClass = syntheticWidthClass,
    )
}

@DevicePreviews
@Composable
internal fun HomeEmpty(
    darkTheme: Boolean = isSystemInDarkTheme(),
    syntheticWidthClass: WidthClass = previewWidthClass(),
) {
    ListScreenPreview(
        screenState = HomeState(),
        darkTheme = darkTheme,
        syntheticWidthClass = syntheticWidthClass,
    )
}

@DevicePreviews
@Composable
internal fun HomeError(
    darkTheme: Boolean = isSystemInDarkTheme(),
    syntheticWidthClass: WidthClass = previewWidthClass(),
) {
    ListScreenPreview(
        screenState = homeErrorState(),
        darkTheme = darkTheme,
        syntheticWidthClass = syntheticWidthClass,
    )
}

/**
 * A settled plan: one full-width hero row and one multi-item scroller.
 *
 * Built from [FakeModelGenerator] rather than collected from
 * [net.sigmabeta.sage.sample.feature.home.HomeViewModel] so the golden is independent of DI,
 * repositories and the clock-seeded daily pick.
 */
private fun homeContentState(): HomeState {
    val generator = FakeModelGenerator()
    val items = generator.randomHomeItems(HERO_AND_GRID_ITEM_COUNT)
    val hero = items.first()
    val grid = items.drop(1)

    return HomeState(
        sections = persistentListOf(
            HomeSectionState(
                id = BIG_IMAGE_ID,
                priority = BIG_IMAGE_PRIORITY,
                lce = LCE.Content(
                    HomeModuleSection(
                        title = SampleStringProvider.getStringOneInt(
                            SampleStringId.HOME_SECTION_BIG_IMAGE,
                            BIG_IMAGE_INSTANCE,
                        ),
                        items = persistentListOf(heroItem(hero)),
                    ),
                ),
                hasEmitted = true,
            ),
            HomeSectionState(
                id = GRID_IMAGES_ID,
                priority = GRID_IMAGES_PRIORITY,
                lce = LCE.Content(
                    HomeModuleSection(
                        title = SampleStringProvider.getStringOneInt(
                            SampleStringId.HOME_SECTION_GRID_IMAGES,
                            GRID_IMAGES_INSTANCE,
                        ),
                        items = grid.map { gridItem(it) }.toImmutableList(),
                    ),
                ),
                hasEmitted = true,
            ),
        ),
    )
}

private fun homeLoadingState(): HomeState = HomeState(
    sections = persistentListOf(
        HomeSectionState(
            id = BIG_IMAGE_ID,
            priority = BIG_IMAGE_PRIORITY,
            lce = LCE.Loading("home.$BIG_IMAGE_ID.load"),
            hasEmitted = true,
        ),
        HomeSectionState(
            id = GRID_IMAGES_ID,
            priority = GRID_IMAGES_PRIORITY,
            lce = LCE.Loading("home.$GRID_IMAGES_ID.load"),
            hasEmitted = true,
        ),
    ),
)

private fun homeErrorState(): HomeState = HomeState(
    sections = persistentListOf(
        HomeSectionState(
            id = BIG_IMAGE_ID,
            priority = BIG_IMAGE_PRIORITY,
            lce = LCE.Error(
                "home.$BIG_IMAGE_ID.load",
                IllegalStateException("Screenshot fixture"),
            ),
            hasEmitted = true,
        ),
    ),
)

private fun heroItem(item: HomeItem): GridImageListModel = GridImageListModel(
    dataId = item.id,
    name = item.title,
    sourceInfo = SourceInfo("screenshot://home/hero/${item.id}"),
    imagePlaceholder = Icon.Album,
    clickAction = SageAction.Noop,
    aspectRatio = HERO_ASPECT_RATIO,
    maxWidthDp = HERO_MAX_WIDTH_DP,
)

private fun gridItem(item: HomeItem): GridImageListModel = GridImageListModel(
    dataId = item.id,
    name = item.title,
    sourceInfo = SourceInfo("screenshot://home/grid/${item.id}"),
    imagePlaceholder = Icon.Album,
    clickAction = SageAction.Noop,
    aspectRatio = GRID_ASPECT_RATIO,
)

private const val BIG_IMAGE_ID = "big_image_1"
private const val BIG_IMAGE_PRIORITY = 100
private const val BIG_IMAGE_INSTANCE = 1
private const val GRID_IMAGES_ID = "grid_images_1"
private const val GRID_IMAGES_PRIORITY = 200
private const val GRID_IMAGES_INSTANCE = 1
private const val GRID_ITEM_COUNT = 4
private const val HERO_AND_GRID_ITEM_COUNT = GRID_ITEM_COUNT + 1

private const val HERO_ASPECT_RATIO = 0.75f
private const val HERO_MAX_WIDTH_DP = 400f
private const val GRID_ASPECT_RATIO = 0.75f

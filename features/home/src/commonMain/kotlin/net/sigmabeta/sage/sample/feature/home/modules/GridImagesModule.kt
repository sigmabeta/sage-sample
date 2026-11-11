package net.sigmabeta.sage.sample.feature.home.modules
import net.sigmabeta.sage.sample.strings.api.SampleStringId
import net.sigmabeta.sage.sample.models.ArtworkCorner
import net.sigmabeta.sage.sample.models.ContentArtworkSize
import net.sigmabeta.sage.sample.models.HomeItem

import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.components.GridImageListModel
import net.sigmabeta.sage.sample.feature.home.HomeAction
import net.sigmabeta.sage.sample.feature.home.HomeModule
import net.sigmabeta.sage.sample.feature.home.HomeModuleSection
import net.sigmabeta.sage.sample.repository.HomeRepository
import net.sigmabeta.sage.sample.feature.home.homeContentId
import net.sigmabeta.sage.sample.feature.home.homeImageSource
import net.sigmabeta.sage.ui.Icon
import net.sigmabeta.sage.ui.StringProvider

/**
 * A horizontal row of grid images.
 *
 * The module asks the repository for a bounded list and hides itself until at least [MIN_ITEMS]
 * are available, so a nearly empty row never flashes onto the screen. Multi-item sections are
 * wrapped in a horizontal scroller by [HomeState].
 *
 * [instanceNumber] and [dataIdBase] come from [net.sigmabeta.sage.sample.feature.home.HomeModuleFactory]:
 * multiple GridImages rows can share the screen without colliding in the section header or item
 * ids. The repository also takes this module's [id] as its content seed, so each instance gets
 * its own catalog and artwork. The sample has no playback history, so the generated catalog stands
 * in for the play-count join.
 */
class GridImagesModule(
    override val id: String,
    override val priority: Int,
    private val instanceNumber: Int,
    private val dataIdBase: Long,
    private val repository: HomeRepository,
    private val stringProvider: StringProvider,
) : HomeModule {

    private val loadOperation = "home.$id.load"

    override fun state(): Flow<LCE<HomeModuleSection>> = flow {
        emit(LCE.Loading(loadOperation))
        emitAll(gridContent())
    }.catch { emit(LCE.Error(loadOperation, it)) }

    private fun gridContent(): Flow<LCE<HomeModuleSection>> =
        repository.gridImages(seed = id, limit = QUERY_LIMIT).map { imagesData ->
            when (imagesData) {
                LCE.Uninitialized -> LCE.Uninitialized
                is LCE.Loading -> LCE.Loading(loadOperation)
                is LCE.Error -> LCE.Error(loadOperation, imagesData.error)
                is LCE.Content -> sectionOrHidden(imagesData.data)
            }
        }

    private fun sectionOrHidden(items: List<HomeItem>): LCE<HomeModuleSection> {
        if (items.size < MIN_ITEMS) return LCE.Uninitialized
        return LCE.Content(
            HomeModuleSection(
                title = stringProvider.getStringOneInt(
                    SampleStringId.HOME_SECTION_GRID_IMAGES,
                    instanceNumber,
                ),
                items = items.map { item ->
                    GridImageListModel(
                        dataId = dataIdBase + item.id,
                        name = item.title,
                        sourceInfo = homeImageSource(
                            item = item,
                            moduleId = id,
                            width = GRID_IMAGE_WIDTH,
                            height = GRID_IMAGE_HEIGHT,
                        ),
                        imagePlaceholder = Icon.Album,
                        clickAction = HomeAction.ItemClicked(
                            contentId = homeContentId(id, item.id),
                            artwork = ContentArtworkSize(
                                width = GRID_IMAGE_WIDTH,
                                height = GRID_IMAGE_HEIGHT,
                                corner = ArtworkCorner.Rounded(CELL_CORNER_RADIUS_DP),
                            ),
                        ),
                        aspectRatio = GRID_IMAGE_ASPECT_RATIO,
                    )
                }.toImmutableList(),
            ),
        )
    }

    private companion object {
        const val QUERY_LIMIT = 10
        const val MIN_ITEMS = 3

        const val GRID_IMAGE_ASPECT_RATIO = 0.75f

        // A 3:4 grid cell at a little over twice the 160 dp column width.
        const val GRID_IMAGE_WIDTH = 264
        const val GRID_IMAGE_HEIGHT = 352

        /** The cell's corner radius, so the detail's clip starts from the cell's shape. */
        const val CELL_CORNER_RADIUS_DP = 16
    }
}

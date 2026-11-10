package net.sigmabeta.sage.sample.feature.home.modules
import net.sigmabeta.sage.sample.strings.api.SampleStringId
import net.sigmabeta.sage.sample.models.ContentArtworkSize
import net.sigmabeta.sage.sample.models.HomeItem

import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
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
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Surfaces a single date-seeded hero item.
 *
 * The seed is the current epoch-day (UTC), so the pick is stable across in-day re-mounts and
 * rolls over once per day. The section emits exactly one item, which makes [HomeState] render it
 * full-width rather than wrapping it in a horizontal scroller; [GridImageListModel.maxWidthDp]
 * keeps that one tile from stretching across a tablet or desktop window.
 *
 * [instanceNumber] and [dataIdBase] come from [net.sigmabeta.sage.sample.feature.home.HomeModuleFactory]:
 * multiple BigImage rows can share the screen without colliding in the section header or item
 * ids. The repository also takes this module's [id] as its content seed, so each instance gets
 * its own catalog and artwork. The catalog is the sample's generated [HomeRepository] instead of a
 * library database.
 */
class BigImageModule(
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
        emit(pickOfTheDay())
    }.catch { emit(LCE.Error(loadOperation, it)) }

    /**
     * Picks one item deterministically for the day with a count + indexed fetch — no need to
     * load the catalog. Hidden (Uninitialized) when the catalog is empty.
     */
    @OptIn(ExperimentalTime::class)
    private suspend fun pickOfTheDay(): LCE<HomeModuleSection> {
        val index = dailyIndex(
            epochDay = Clock.System.now().toEpochMilliseconds() / MILLIS_PER_DAY,
            itemCount = repository.itemCount(seed = id),
        ) ?: return LCE.Uninitialized
        val pick = repository.itemAt(seed = id, index = index) ?: return LCE.Uninitialized
        return sectionFor(pick)
    }

    private fun sectionFor(pick: HomeItem): LCE<HomeModuleSection> = LCE.Content(
        HomeModuleSection(
            title = stringProvider.getStringOneInt(
                SampleStringId.HOME_SECTION_BIG_IMAGE,
                instanceNumber,
            ),
            items = persistentListOf(
                GridImageListModel(
                    dataId = dataIdBase + pick.id,
                    name = pick.title,
                    sourceInfo = homeImageSource(
                        item = pick,
                        moduleId = id,
                        width = HERO_IMAGE_WIDTH,
                        height = HERO_IMAGE_HEIGHT,
                    ),
                    imagePlaceholder = Icon.Album,
                    clickAction = HomeAction.ItemClicked(
                        contentId = homeContentId(id, pick.id),
                        artwork = ContentArtworkSize(HERO_IMAGE_WIDTH, HERO_IMAGE_HEIGHT),
                    ),
                    aspectRatio = HERO_ASPECT_RATIO,
                    maxWidthDp = MAX_WIDTH_DP,
                ),
            ),
        ),
    )

    private companion object {
        const val HERO_ASPECT_RATIO = 0.75f

        // Cap the full-width hero so it stays a reasonable size on tablets / desktop.
        const val MAX_WIDTH_DP = 400f

        // The hero asks for the source resolution, so the URL carries an explicit 3:4 cover
        // size (IGDB's 528 × 704) rather than relying on the display size.
        const val HERO_IMAGE_WIDTH = 528
        const val HERO_IMAGE_HEIGHT = 704

        const val MILLIS_PER_DAY = 86_400_000L
    }
}

/**
 * Maps an [epochDay] onto an index in `[0, itemCount)`, or null for an empty catalog.
 *
 * The modulo is floored, so a negative day (before the epoch) still lands in range; tests pin
 * the zero, first, last and rollover values.
 */
internal fun dailyIndex(epochDay: Long, itemCount: Int): Int? =
    if (itemCount <= 0) null else epochDay.mod(itemCount.toLong()).toInt()

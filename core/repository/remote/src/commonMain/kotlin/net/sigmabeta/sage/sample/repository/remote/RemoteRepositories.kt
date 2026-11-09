package net.sigmabeta.sage.sample.repository.remote

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.di.AppScope
import net.sigmabeta.sage.sample.models.ContentDetail
import net.sigmabeta.sage.sample.models.ContentId
import net.sigmabeta.sage.sample.models.HomeItem
import net.sigmabeta.sage.sample.models.SampleItem
import net.sigmabeta.sage.sample.models.SearchResult
import net.sigmabeta.sage.sample.repository.ContentDetailRepository
import net.sigmabeta.sage.sample.repository.ContentDetailResponse
import net.sigmabeta.sage.sample.repository.GRID_OF_IMAGES_OPERATION_NAME
import net.sigmabeta.sage.sample.repository.GridOfImagesRepository
import net.sigmabeta.sage.sample.repository.HomeCountResponse
import net.sigmabeta.sage.sample.repository.HomeItemResponse
import net.sigmabeta.sage.sample.repository.HomeItemsResponse
import net.sigmabeta.sage.sample.repository.HomeRepository
import net.sigmabeta.sage.sample.repository.LIST_ITEMS_OPERATION_NAME
import net.sigmabeta.sage.sample.repository.SEARCH_GRID_OPERATION_NAME
import net.sigmabeta.sage.sample.repository.SEARCH_LIST_OPERATION_NAME
import net.sigmabeta.sage.sample.repository.SampleApiParams
import net.sigmabeta.sage.sample.repository.SampleApiRoutes
import net.sigmabeta.sage.sample.repository.SampleItemRepository
import net.sigmabeta.sage.sample.repository.SampleItemsResponse
import net.sigmabeta.sage.sample.repository.SearchRepository
import net.sigmabeta.sage.sample.repository.SearchResultKind
import net.sigmabeta.sage.sample.repository.SearchResultsResponse
import net.sigmabeta.sage.sample.repository.toDomain
import net.sigmabeta.sage.sample.repository.toDomainOrNull
import net.sigmabeta.sage.sample.settings.DemoOutcome
import net.sigmabeta.sage.sample.settings.SampleSettingsManager
import net.sigmabeta.sage.sample.settings.demoOutcomeFailure

/**
 * HTTP implementations of the sample's repository seams for the browser client.
 *
 * The server always returns content; [DemoOutcome.EMPTY] and [DemoOutcome.ERROR] are applied here,
 * on the client, because they are a Settings simulation switch rather than a server concern. The
 * outcome is read when a load is collected, matching the in-process demo sources.
 */

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class RemoteHomeRepository(
    private val api: SampleApiClient,
    private val settingsManager: SampleSettingsManager,
) : HomeRepository {
    override suspend fun itemCount(seed: String): Int = when (settingsManager.demoOutcome.value) {
        DemoOutcome.CONTENT -> api.get<HomeCountResponse>(
            SampleApiRoutes.HOME_COUNT,
            mapOf(SampleApiParams.SEED to seed),
        ).count

        DemoOutcome.EMPTY -> 0

        DemoOutcome.ERROR -> throw demoOutcomeFailure()
    }

    override suspend fun itemAt(seed: String, index: Int): HomeItem? =
        when (settingsManager.demoOutcome.value) {
            DemoOutcome.CONTENT -> api.get<HomeItemResponse>(
                SampleApiRoutes.HOME_ITEM,
                mapOf(SampleApiParams.SEED to seed, SampleApiParams.INDEX to index.toString()),
            ).item?.toDomain()

            DemoOutcome.EMPTY -> null

            DemoOutcome.ERROR -> throw demoOutcomeFailure()
        }

    override fun gridImages(seed: String, limit: Int): Flow<LCE<List<HomeItem>>> = flow {
        emit(
            loadList("home.$seed.load", settingsManager.demoOutcome.value) {
                api.get<HomeItemsResponse>(
                    SampleApiRoutes.HOME_GRID,
                    mapOf(SampleApiParams.SEED to seed, SampleApiParams.LIMIT to limit.toString()),
                ).items.map { it.toDomain() }
            },
        )
    }
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class RemoteContentDetailRepository(
    private val api: SampleApiClient,
) : ContentDetailRepository {
    override fun load(contentId: ContentId): Flow<LCE<ContentDetail?>> = flow {
        val response = api.get<ContentDetailResponse>(
            SampleApiRoutes.CONTENT_DETAIL,
            mapOf(SampleApiParams.CONTENT_ID to contentId.value),
        )
        emit(LCE.Content(response.detail?.toDomainOrNull()))
    }
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class RemoteSampleItemRepository(
    private val api: SampleApiClient,
    private val settingsManager: SampleSettingsManager,
) : SampleItemRepository {
    override fun load(offset: Int, limit: Int): Flow<LCE<List<SampleItem>>> = flow {
        emit(
            loadList(LIST_ITEMS_OPERATION_NAME, settingsManager.demoOutcome.value) {
                api.get<SampleItemsResponse>(
                    SampleApiRoutes.LIST_ITEMS,
                    paging(offset, limit),
                ).items.map { it.toDomain() }
            },
        )
    }
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class RemoteGridOfImagesRepository(
    private val api: SampleApiClient,
    private val settingsManager: SampleSettingsManager,
) : GridOfImagesRepository {
    override fun load(offset: Int, limit: Int): Flow<LCE<List<SampleItem>>> = flow {
        emit(
            loadList(GRID_OF_IMAGES_OPERATION_NAME, settingsManager.demoOutcome.value) {
                api.get<SampleItemsResponse>(
                    SampleApiRoutes.GRID_OF_IMAGES,
                    paging(offset, limit),
                ).items.map { it.toDomain() }
            },
        )
    }
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class RemoteSearchRepository(
    private val api: SampleApiClient,
    private val settingsManager: SampleSettingsManager,
) : SearchRepository {
    override fun searchList(query: String): Flow<LCE<List<SearchResult>>> =
        resultsFor(query, SearchResultKind.LIST, SEARCH_LIST_OPERATION_NAME)

    override fun searchGrid(query: String): Flow<LCE<List<SearchResult>>> =
        resultsFor(query, SearchResultKind.GRID, SEARCH_GRID_OPERATION_NAME)

    private fun resultsFor(
        query: String,
        kind: SearchResultKind,
        operationName: String,
    ): Flow<LCE<List<SearchResult>>> = flow {
        emit(
            loadList(operationName, settingsManager.demoOutcome.value) {
                api.get<SearchResultsResponse>(
                    SampleApiRoutes.SEARCH,
                    mapOf(
                        SampleApiParams.QUERY to query,
                        SampleApiParams.KIND to kind.routeSegment,
                    ),
                ).results.map { it.toDomain() }
            },
        )
    }
}

/** One page of List Items / Grid items as query parameters. */
private fun paging(offset: Int, limit: Int): Map<String, String> = mapOf(
    SampleApiParams.OFFSET to offset.toString(),
    SampleApiParams.LIMIT to limit.toString(),
)

/** Applies the demo outcome to a suspending list load. */
private suspend fun <T> loadList(
    operationName: String,
    outcome: DemoOutcome,
    load: suspend () -> List<T>,
): LCE<List<T>> = when (outcome) {
    DemoOutcome.CONTENT -> LCE.Content(load())
    DemoOutcome.EMPTY -> LCE.Content(emptyList())
    DemoOutcome.ERROR -> LCE.Error(operationName, demoOutcomeFailure())
}

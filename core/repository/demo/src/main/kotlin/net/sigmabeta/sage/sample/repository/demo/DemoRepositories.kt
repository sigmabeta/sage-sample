package net.sigmabeta.sage.sample.repository.demo

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
import net.sigmabeta.sage.sample.repository.GRID_OF_IMAGES_ITEM_COUNT
import net.sigmabeta.sage.sample.repository.GRID_OF_IMAGES_OPERATION_NAME
import net.sigmabeta.sage.sample.repository.GridOfImagesRepository
import net.sigmabeta.sage.sample.repository.HomeRepository
import net.sigmabeta.sage.sample.repository.LIST_ITEMS_ITEM_COUNT
import net.sigmabeta.sage.sample.repository.LIST_ITEMS_OPERATION_NAME
import net.sigmabeta.sage.sample.repository.SEARCH_GRID_OPERATION_NAME
import net.sigmabeta.sage.sample.repository.SEARCH_LIST_OPERATION_NAME
import net.sigmabeta.sage.sample.repository.SampleItemRepository
import net.sigmabeta.sage.sample.repository.SearchRepository
import net.sigmabeta.sage.sample.repository.SearchResultKind
import net.sigmabeta.sage.sample.settings.DemoOutcome
import net.sigmabeta.sage.sample.settings.SampleSettingsManager
import net.sigmabeta.sage.sample.settings.demoOutcomeFailure
import net.sigmabeta.sage.sample.settings.select

/**
 * In-process implementations of the sample's repository seams.
 *
 * The Android and desktop apps bind these; the browser binds the HTTP-backed implementations in
 * `:core:repository:remote`. Every class reads the Settings demo outcome when a load is requested
 * (not at construction), so [DemoOutcome.EMPTY] and [DemoOutcome.ERROR] reach the screens'
 * terminal states without an app restart.
 *
 * The content itself comes from [DemoContent], which `apps:server` also uses. That shared generator
 * is what keeps the local demo and the web client showing identical rows for the same seed.
 */

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class DemoHomeRepository(
    private val settingsManager: SampleSettingsManager,
) : HomeRepository {
    override suspend fun itemCount(seed: String): Int =
        settingsManager.demoOutcome.value.select(
            content = { DemoContent.homeItemCount(seed) },
            empty = { 0 },
            error = { throw demoOutcomeFailure() },
        )

    override suspend fun itemAt(seed: String, index: Int): HomeItem? =
        settingsManager.demoOutcome.value.select(
            content = { DemoContent.homeItem(seed, index) },
            empty = { null },
            error = { throw demoOutcomeFailure() },
        )

    override fun gridImages(seed: String, limit: Int): Flow<LCE<List<HomeItem>>> = flow {
        emit(
            settingsManager.demoOutcome.value.select(
                content = { LCE.Content(DemoContent.homeGrid(seed, limit)) },
                empty = { LCE.Content(emptyList()) },
                error = { LCE.Error("home.$seed.load", demoOutcomeFailure()) },
            ),
        )
    }
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class DemoContentDetailRepository : ContentDetailRepository {
    override fun load(contentId: ContentId): Flow<LCE<ContentDetail?>> = flow {
        emit(LCE.Content(DemoContent.contentDetail(contentId)))
    }
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class DemoSampleItemRepository(
    private val settingsManager: SampleSettingsManager,
) : SampleItemRepository {
    override fun load(offset: Int, limit: Int): Flow<LCE<List<SampleItem>>> {
        val outcome = settingsManager.demoOutcome.value

        return flow {
            emit(
                outcome.select(
                    content = {
                        LCE.Content(
                            DemoContent.sampleItems(
                                offset = offset,
                                limit = limit,
                                total = LIST_ITEMS_ITEM_COUNT,
                            ),
                        )
                    },
                    empty = { LCE.Content(emptyList()) },
                    error = {
                        LCE.Error(
                            operationName = LIST_ITEMS_OPERATION_NAME,
                            error = demoOutcomeFailure(),
                        )
                    },
                ),
            )
        }
    }
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class DemoGridOfImagesRepository(
    private val settingsManager: SampleSettingsManager,
) : GridOfImagesRepository {
    override fun load(offset: Int, limit: Int): Flow<LCE<List<SampleItem>>> {
        val outcome = settingsManager.demoOutcome.value

        return flow {
            emit(
                outcome.select(
                    content = {
                        LCE.Content(
                            DemoContent.sampleItems(
                                offset = offset,
                                limit = limit,
                                total = GRID_OF_IMAGES_ITEM_COUNT,
                            ),
                        )
                    },
                    empty = { LCE.Content(emptyList()) },
                    error = {
                        LCE.Error(
                            operationName = GRID_OF_IMAGES_OPERATION_NAME,
                            error = demoOutcomeFailure(),
                        )
                    },
                ),
            )
        }
    }
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class DemoSearchRepository(
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
    ): Flow<LCE<List<SearchResult>>> {
        val outcome = settingsManager.demoOutcome.value

        return flow {
            emit(
                outcome.select(
                    content = { LCE.Content(DemoContent.searchResults(query, kind)) },
                    empty = { LCE.Content(emptyList()) },
                    error = {
                        LCE.Error(
                            operationName = operationName,
                            error = demoOutcomeFailure(),
                        )
                    },
                ),
            )
        }
    }
}

package net.sigmabeta.sage.sample.feature.listitems
import net.sigmabeta.sage.sample.models.SampleItem

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.sigmabeta.sage.analytics.Analytics
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.coroutines.SageDispatchers
import net.sigmabeta.sage.list.ListState
import net.sigmabeta.sage.sample.settings.SampleDelayManager
import net.sigmabeta.sage.sample.viewmodel.SampleListViewModel
import net.sigmabeta.sage.ui.StringProvider

/**
 * Shared paging behaviour for the sample's paginated List Items screens.
 *
 * It mirrors Chipbox's `BrowseAllTracksViewModel`: the screen opens one window at an offset, then
 * grows it in both directions. A single [pageJob] serializes requests, so the burst of scroll
 * signals SAGE emits while a page is in flight is coalesced into one fetch. Appends keep the
 * existing rows on failure and dedupe by [SampleItem.id]; a short or empty page marks the window's
 * end so no further requests are made.
 *
 * The state-specific parts are the four small hooks below: how to read the LCE content, window
 * start and end-of-content flag, and how to copy a new window into the concrete state.
 */
abstract class PagedListItemsViewModel<StateType : ListState>(
    override val stringProvider: StringProvider,
    override val analytics: Analytics,
    override val dispatchers: SageDispatchers,
    override val delayManager: SampleDelayManager,
    private val operationName: String,
) : SampleListViewModel<StateType>() {
    /**
     * Only one page request runs at a time. Append and prepend both mutate the window list, and
     * the active check coalesces the burst of scroll signals emitted while a page is in flight.
     */
    private var pageJob: Job? = null

    /** Rows requested per page; must match the state's `PaginationType.Standard.pageSize`. */
    protected abstract val pageSize: Int

    /** Fetches one page window from the screen's repository. */
    protected abstract suspend fun fetchPage(offset: Int, limit: Int): LCE<List<SampleItem>>

    protected abstract fun contentOf(state: StateType): LCE<List<SampleItem>>

    protected abstract fun windowStartOf(state: StateType): Int

    protected abstract fun hasMoreAfterOf(state: StateType): Boolean

    protected abstract fun withPaging(
        state: StateType,
        content: LCE<List<SampleItem>>,
        windowStart: Int,
        hasMoreAfter: Boolean,
        loadingMore: Boolean,
        loadingPrevious: Boolean,
    ): StateType

    /**
     * Handles the paging actions every paginated screen shares.
     *
     * Returns `true` when [action] was one of them, so a screen's `handleAction` can fall through
     * to its own row actions only for the remainder.
     */
    protected fun handlePagingAction(action: SageAction): Boolean = when (action) {
        SageAction.InitNoArgs -> {
            loadInitial(startOffset = 0)
            true
        }

        // Entry-at-offset seam: open the window around a target page. No route sends this yet; it
        // is the hook for a future "resume where I left off" entry, and the tests use it to
        // exercise prepending from a non-zero window.
        is SageAction.InitWithPageNumber -> {
            val offset = (action.pageNumber * pageSize).coerceAtMost(Int.MAX_VALUE.toLong())
            loadInitial(startOffset = offset.toInt())
            true
        }

        SageAction.LoadMoreRequested -> {
            loadNextPage()
            true
        }

        SageAction.LoadPreviousRequested -> {
            loadPreviousPage()
            true
        }

        else -> false
    }

    /** (Re)opens the window at [startOffset], discarding anything previously loaded. */
    private fun loadInitial(startOffset: Int) {
        pageJob?.cancel()
        updateState {
            withPaging(
                state = it,
                content = LCE.Loading(operationName),
                windowStart = startOffset,
                hasMoreAfter = true,
                loadingMore = false,
                loadingPrevious = false,
            )
        }
        pageJob = viewModelScope.launch {
            val page = fetch(offset = startOffset, limit = pageSize)
            updateState { reduceInitial(it, page) }
        }
    }

    /** Appends the next page below the current window. */
    private fun loadNextPage() {
        if (pageJob?.isActive == true) return

        val current = internalUiState.value
        val loaded = (contentOf(current) as? LCE.Content)?.data ?: return
        if (!hasMoreAfterOf(current)) return

        val offset = windowStartOf(current) + loaded.size
        updateState {
            withPaging(
                state = it,
                content = contentOf(it),
                windowStart = windowStartOf(it),
                hasMoreAfter = hasMoreAfterOf(it),
                loadingMore = true,
                loadingPrevious = false,
            )
        }
        pageJob = viewModelScope.launch {
            val page = fetch(offset = offset, limit = pageSize)
            updateState { reduceAppend(it, page) }
        }
    }

    /** Prepends the previous page above the current window; a window at offset zero is a no-op. */
    private fun loadPreviousPage() {
        if (pageJob?.isActive == true) return

        val current = internalUiState.value
        if (contentOf(current) !is LCE.Content) return
        val windowStart = windowStartOf(current)
        if (windowStart <= 0) return

        val newStart = (windowStart - pageSize).coerceAtLeast(0)
        val count = windowStart - newStart
        if (count <= 0) return

        updateState {
            withPaging(
                state = it,
                content = contentOf(it),
                windowStart = windowStartOf(it),
                hasMoreAfter = hasMoreAfterOf(it),
                loadingMore = false,
                loadingPrevious = true,
            )
        }
        pageJob = viewModelScope.launch {
            val page = fetch(offset = newStart, limit = count)
            updateState { reducePrepend(it, page, newStart) }
        }
    }

    private fun reduceInitial(
        state: StateType,
        page: LCE<List<SampleItem>>,
    ): StateType = when (page) {
        is LCE.Content -> withPaging(
            state = state,
            content = LCE.Content(page.data),
            windowStart = windowStartOf(state),
            hasMoreAfter = page.data.size >= pageSize,
            loadingMore = false,
            loadingPrevious = false,
        )

        is LCE.Error -> withPaging(
            state = state,
            content = page,
            windowStart = windowStartOf(state),
            hasMoreAfter = false,
            loadingMore = false,
            loadingPrevious = false,
        )

        is LCE.Loading,
        LCE.Uninitialized -> state
    }

    private fun reduceAppend(
        state: StateType,
        page: LCE<List<SampleItem>>,
    ): StateType {
        val existing = (contentOf(state) as? LCE.Content)?.data.orEmpty()

        return when (page) {
            is LCE.Content -> withPaging(
                state = state,
                content = LCE.Content(appendDistinct(existing, page.data)),
                windowStart = windowStartOf(state),
                hasMoreAfter = page.data.size >= pageSize,
                loadingMore = false,
                loadingPrevious = false,
            )

            // Keep what is already shown; just drop the footer loader.
            is LCE.Error -> withPaging(
                state = state,
                content = LCE.Content(existing),
                windowStart = windowStartOf(state),
                hasMoreAfter = hasMoreAfterOf(state),
                loadingMore = false,
                loadingPrevious = false,
            )

            is LCE.Loading,
            LCE.Uninitialized -> state
        }
    }

    private fun reducePrepend(
        state: StateType,
        page: LCE<List<SampleItem>>,
        newStart: Int,
    ): StateType {
        val existing = (contentOf(state) as? LCE.Content)?.data.orEmpty()

        return when (page) {
            is LCE.Content -> withPaging(
                state = state,
                content = LCE.Content(prependDistinct(page.data, existing)),
                windowStart = newStart,
                hasMoreAfter = hasMoreAfterOf(state),
                loadingMore = false,
                loadingPrevious = false,
            )

            // Keep what is already shown; just drop the header loader.
            is LCE.Error -> withPaging(
                state = state,
                content = LCE.Content(existing),
                windowStart = windowStartOf(state),
                hasMoreAfter = hasMoreAfterOf(state),
                loadingMore = false,
                loadingPrevious = false,
            )

            is LCE.Loading,
            LCE.Uninitialized -> state
        }
    }

    /**
     * Fetches a page through the sample's single asynchronous seam: the Settings delay, then the
     * repository on the disk dispatcher. Runs inside [pageJob], so [viewModelScope] cancels it when
     * the screen's ViewModel is cleared.
     */
    private suspend fun fetch(offset: Int, limit: Int): LCE<List<SampleItem>> {
        val delayMillis = delayManager.delayMillis()
        if (delayMillis > 0L) delay(delayMillis)

        return withContext(dispatchers.disk) {
            fetchPage(offset = offset, limit = limit)
        }
    }
}

/** Appends [page] below [existing], keeping one row per [SampleItem.id]. */
internal fun appendDistinct(
    existing: List<SampleItem>,
    page: List<SampleItem>,
): List<SampleItem> = (existing + page).distinctBy { it.id }

/** Prepends [page] above [existing], keeping one row per [SampleItem.id]. */
internal fun prependDistinct(
    page: List<SampleItem>,
    existing: List<SampleItem>,
): List<SampleItem> = (page + existing).distinctBy { it.id }

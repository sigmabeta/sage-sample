package net.sigmabeta.sage.sample.repository

import kotlinx.serialization.Serializable
import net.sigmabeta.sage.sample.models.ContentAction
import net.sigmabeta.sage.sample.models.ContentDetail
import net.sigmabeta.sage.sample.models.ContentDetailField
import net.sigmabeta.sage.sample.models.ContentId
import net.sigmabeta.sage.sample.models.ContentItem
import net.sigmabeta.sage.sample.models.HomeItem
import net.sigmabeta.sage.sample.models.RelatedContent
import net.sigmabeta.sage.sample.models.SampleItem
import net.sigmabeta.sage.sample.models.SearchResult
import net.sigmabeta.sage.ui.Icon

// The JSON contract between `apps:server` and the browser client.
//
// Wire models are deliberately separate from the domain models: they carry an icon as its stable
// name instead of SAGE's sealed Icon type, and they can evolve without changing renderer input.
// Both sides call the same toDomain/toDto functions, so a shape change cannot drift.
//
// Paths and query parameter names live in SampleApiRoutes / SampleApiParams so the server and the
// HTTP repository cannot disagree about them.

/** The JSON API's path namespace. Every path is absolute and starts with `/api`. */
object SampleApiRoutes {
    const val HEALTH = "/api/health"

    const val HOME_COUNT = "/api/home/count"
    const val HOME_ITEM = "/api/home/item"
    const val HOME_GRID = "/api/home/grid"

    const val CONTENT_DETAIL = "/api/content"

    const val LIST_ITEMS = "/api/list-items"
    const val GRID_OF_IMAGES = "/api/grid-of-images"

    const val SEARCH = "/api/search"
}

/** Query parameter names shared by the server routes and the browser client. */
object SampleApiParams {
    const val SEED = "seed"
    const val INDEX = "index"
    const val LIMIT = "limit"
    const val OFFSET = "offset"
    const val CONTENT_ID = "id"
    const val QUERY = "q"
    const val KIND = "kind"
}

/** Default page size when a request omits `limit`. */
const val DEFAULT_PAGE_LIMIT = 50

/** The largest page a request may ask for; larger values are clamped. */
const val MAX_PAGE_LIMIT = 500

/** Clamps a requested page to the server's bounds. Invalid values fall back to the defaults. */
fun normalizedPage(offset: Int?, limit: Int?): Pair<Int, Int> {
    val safeOffset = (offset ?: 0).coerceAtLeast(0)
    val safeLimit = (limit ?: DEFAULT_PAGE_LIMIT).coerceIn(1, MAX_PAGE_LIMIT)
    return safeOffset to safeLimit
}

@Serializable
data class HomeItemDto(
    val id: Long,
    val title: String,
)

@Serializable
data class HomeItemResponse(val item: HomeItemDto?)

@Serializable
data class HomeItemsResponse(val items: List<HomeItemDto>)

@Serializable
data class HomeCountResponse(val count: Int)

@Serializable
data class SampleItemDto(
    val id: Long,
    val name: String,
    val caption: String,
)

@Serializable
data class SampleItemsResponse(val items: List<SampleItemDto>)

@Serializable
data class SearchResultDto(
    val id: Long,
    val name: String,
    val caption: String,
)

@Serializable
data class SearchResultsResponse(val results: List<SearchResultDto>)

@Serializable
data class ContentDetailFieldDto(
    val label: String,
    val value: String,
)

@Serializable
data class ContentActionDto(
    val label: String,
    val icon: String,
)

@Serializable
data class RelatedContentDto(
    val id: Long,
    val name: String,
)

@Serializable
data class ContentItemDto(
    val id: Long,
    val name: String,
    val caption: String,
)

@Serializable
data class ContentDetailDto(
    val id: String,
    val title: String,
    val details: List<ContentDetailFieldDto>,
    val actions: List<ContentActionDto>,
    val related: List<RelatedContentDto>,
    val items: List<ContentItemDto>,
)

/** A successful content lookup; `null` is the explicit not-found result, not an error. */
@Serializable
data class ContentDetailResponse(val detail: ContentDetailDto?)

@Serializable
data class HealthResponse(val ok: Boolean)

@Serializable
data class ApiErrorResponse(val error: String)

fun HomeItem.toDto(): HomeItemDto = HomeItemDto(id = id, title = title)

fun HomeItemDto.toDomain(): HomeItem = HomeItem(id = id, title = title)

fun SampleItem.toDto(): SampleItemDto = SampleItemDto(id = id, name = name, caption = caption)

fun SampleItemDto.toDomain(): SampleItem = SampleItem(id = id, name = name, caption = caption)

fun SearchResult.toDto(): SearchResultDto = SearchResultDto(id = id, name = name, caption = caption)

fun SearchResultDto.toDomain(): SearchResult = SearchResult(id = id, name = name, caption = caption)

fun ContentDetail.toDto(): ContentDetailDto = ContentDetailDto(
    id = id.value,
    title = title,
    details = details.map { it.toDto() },
    actions = actions.map { it.toDto() },
    related = related.map { it.toDto() },
    items = items.map { it.toDto() },
)

/** Decodes a wire detail, or `null` when the server sent an unusable content id. */
fun ContentDetailDto.toDomainOrNull(): ContentDetail? {
    val contentId = runCatching { ContentId(id) }.getOrNull() ?: return null
    return ContentDetail(
        id = contentId,
        title = title,
        details = details.map { it.toDomain() },
        actions = actions.map { it.toDomain() },
        related = related.map { it.toDomain() },
        items = items.map { it.toDomain() },
    )
}

fun ContentDetailField.toDto(): ContentDetailFieldDto =
    ContentDetailFieldDto(label = label, value = value)

fun ContentDetailFieldDto.toDomain(): ContentDetailField =
    ContentDetailField(label = label, value = value)

fun ContentAction.toDto(): ContentActionDto =
    ContentActionDto(label = label, icon = icon.wireName())

fun ContentActionDto.toDomain(): ContentAction =
    ContentAction(label = label, icon = iconFromWireName(icon) ?: Icon.Description)

fun RelatedContent.toDto(): RelatedContentDto = RelatedContentDto(id = id, name = name)

fun RelatedContentDto.toDomain(): RelatedContent = RelatedContent(id = id, name = name)

fun ContentItem.toDto(): ContentItemDto = ContentItemDto(id = id, name = name, caption = caption)

fun ContentItemDto.toDomain(): ContentItem = ContentItem(id = id, name = name, caption = caption)

/** The stable wire name for an icon; the reverse mapping is [iconFromWireName]. */
fun Icon.wireName(): String = when (this) {
    Icon.Album -> "album"
    Icon.Back -> "back"
    Icon.Browse -> "browse"
    Icon.Calendar -> "calendar"
    Icon.Caret -> "caret"
    Icon.Chip -> "chip"
    Icon.Clear -> "clear"
    Icon.Computer -> "computer"
    Icon.Delete -> "delete"
    Icon.CrossOut -> "cross-out"
    Icon.CrossOutError -> "cross-out-error"
    Icon.CrossOutMenu -> "cross-out-menu"
    Icon.Description -> "description"
    Icon.Difficulty -> "difficulty"
    Icon.Edit -> "edit"
    Icon.FavoriteEmpty -> "favorite-empty"
    Icon.FavoriteFilled -> "favorite-filled"
    Icon.Forward -> "forward"
    Icon.Home -> "home"
    Icon.Library -> "library"
    Icon.Menu -> "menu"
    Icon.Minus -> "minus"
    Icon.MusicNote -> "music-note"
    Icon.OfflineFilled -> "offline-filled"
    Icon.OfflineOutline -> "offline-outline"
    Icon.Overflow -> "overflow"
    Icon.Pause -> "pause"
    Icon.Person -> "person"
    Icon.Play -> "play"
    Icon.Plus -> "plus"
    Icon.QueueMusic -> "queue-music"
    Icon.Refresh -> "refresh"
    Icon.Repeat -> "repeat"
    Icon.RepeatOne -> "repeat-one"
    Icon.Save -> "save"
    Icon.Search -> "search"
    Icon.SearchYoutube -> "search-youtube"
    Icon.Shuffle -> "shuffle"
    Icon.SkipNext -> "skip-next"
    Icon.SkipPrevious -> "skip-previous"
    Icon.Tag -> "tag"
    Icon.Visibility -> "visibility"
    Icon.VisibilityOff -> "visibility-off"
    Icon.Warning -> "warning"
}

/** Resolves a wire icon name, or `null` when a newer server sent one this client lacks. */
fun iconFromWireName(name: String): Icon? = when (name) {
    "album" -> Icon.Album
    "back" -> Icon.Back
    "browse" -> Icon.Browse
    "calendar" -> Icon.Calendar
    "caret" -> Icon.Caret
    "chip" -> Icon.Chip
    "clear" -> Icon.Clear
    "computer" -> Icon.Computer
    "delete" -> Icon.Delete
    "cross-out" -> Icon.CrossOut
    "cross-out-error" -> Icon.CrossOutError
    "cross-out-menu" -> Icon.CrossOutMenu
    "description" -> Icon.Description
    "difficulty" -> Icon.Difficulty
    "edit" -> Icon.Edit
    "favorite-empty" -> Icon.FavoriteEmpty
    "favorite-filled" -> Icon.FavoriteFilled
    "forward" -> Icon.Forward
    "home" -> Icon.Home
    "library" -> Icon.Library
    "menu" -> Icon.Menu
    "minus" -> Icon.Minus
    "music-note" -> Icon.MusicNote
    "offline-filled" -> Icon.OfflineFilled
    "offline-outline" -> Icon.OfflineOutline
    "overflow" -> Icon.Overflow
    "pause" -> Icon.Pause
    "person" -> Icon.Person
    "play" -> Icon.Play
    "plus" -> Icon.Plus
    "queue-music" -> Icon.QueueMusic
    "refresh" -> Icon.Refresh
    "repeat" -> Icon.Repeat
    "repeat-one" -> Icon.RepeatOne
    "save" -> Icon.Save
    "search" -> Icon.Search
    "search-youtube" -> Icon.SearchYoutube
    "shuffle" -> Icon.Shuffle
    "skip-next" -> Icon.SkipNext
    "skip-previous" -> Icon.SkipPrevious
    "tag" -> Icon.Tag
    "visibility" -> Icon.Visibility
    "visibility-off" -> Icon.VisibilityOff
    "warning" -> Icon.Warning
    else -> null
}

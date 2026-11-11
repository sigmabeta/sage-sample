package net.sigmabeta.sage.sample.feature.contentdetail
import net.sigmabeta.sage.sample.strings.api.SampleStringId
import net.sigmabeta.sage.sample.models.ArtworkCorner
import net.sigmabeta.sage.sample.models.ContentArtworkSize
import net.sigmabeta.sage.sample.models.ContentId
import net.sigmabeta.sage.sample.models.ContentDetailField
import net.sigmabeta.sage.sample.models.ContentDetail
import net.sigmabeta.sage.sample.models.contentDetailItemImageSeed
import net.sigmabeta.sage.sample.models.contentDetailRelatedImageSeed
import net.sigmabeta.sage.sample.models.contentHeroImageSeed
import net.sigmabeta.sage.sample.models.contentItemContentId
import net.sigmabeta.sage.sample.models.relatedContentId

import kotlinx.collections.immutable.immutableListOf
import kotlinx.collections.immutable.toImmutableList
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.components.CtaListModel
import net.sigmabeta.sage.components.EmptyStateListModel
import net.sigmabeta.sage.components.HeroImageListModel
import net.sigmabeta.sage.components.HorizontalScrollerListModel
import net.sigmabeta.sage.components.ImageNameCaptionListModel
import net.sigmabeta.sage.components.LabelValueListModel
import net.sigmabeta.sage.components.ListModel
import net.sigmabeta.sage.components.LoadingType
import net.sigmabeta.sage.components.SectionHeaderListModel
import net.sigmabeta.sage.components.SectionListModel
import net.sigmabeta.sage.components.TitleBarModel
import net.sigmabeta.sage.components.WideItemListModel
import net.sigmabeta.sage.images.SourceInfo
import net.sigmabeta.sage.list.ColumnType
import net.sigmabeta.sage.list.ListState
import net.sigmabeta.sage.sample.bitmaps.GeneratedImageFetcher
import net.sigmabeta.sage.ui.Icon
import net.sigmabeta.sage.ui.StringProvider
import net.sigmabeta.sage.sample.repository.CONTENT_DETAIL_OPERATION_NAME

/**
 * Content detail's content, declared as a SAGE [ListState].
 *
 * A hero image, a column of generated call-to-action rows, a block of generated label/value
 * rows, a horizontally scrolling related row, and a list of generated image/name/caption items.
 * Each section maps the same [LCE] through its own loading shape, so the placeholder for a
 * section looks like the content that replaces it.
 *
 * [content]'s `null` payload is the explicit not-found state: the repository reports an unknown
 * [ContentId] as `LCE.Content(null)` and the screen renders one localized explanation instead of
 * an error or an empty list. The title keeps the destination fallback in that case.
 *
 * The [ColumnType.Staggered] declaration is what makes the shared screen render SAGE's staggered
 * `GridScreen` once the window is wide enough for more than one column; compact windows keep the
 * single-column list presentation.
 */
data class ContentDetailState(
    val content: LCE<ContentDetail?> = LCE.Loading(CONTENT_DETAIL_OPERATION_NAME),
    /**
     * The address this screen was opened with, known before [content] arrives.
     *
     * The hero is seeded by the address, not by the payload (see [contentHeroImageSeed]), so
     * knowing it during the load is enough to render the detail's real hero instead of a loading
     * row. `null` until the screen's init action lands, and in the not-found state.
     */
    val contentId: ContentId? = null,
) : ListState() {
    override val columnType = ColumnType.Staggered(CONTENT_DETAIL_COLUMN_WIDTH_DP, false)

    /**
     * The loaded item's title, falling back to the destination name while loading, on error, or
     * when the addressed ID is unknown. The screen is always pushed, so it is below the top level
     * and shows a back arrow — including in the not-found state.
     */
    override fun title(stringProvider: StringProvider): TitleBarModel {
        val loadedTitle = (content as? LCE.Content)?.data?.title
        return TitleBarModel(
            title = loadedTitle
                ?: stringProvider.getString(SampleStringId.CONTENT_DETAIL_TITLE),
            shouldShowBack = true,
        )
    }

    override fun toListItems(stringProvider: StringProvider): List<ListModel> =
        when (val loaded = content) {
            is LCE.Error -> error(CONTENT_DETAIL_OPERATION_NAME, loaded.error)

            is LCE.Content ->
                loaded.data
                ?.let { detail -> sections(LCE.Content(detail), stringProvider) }
                ?: listOf(notFound(stringProvider))

            is LCE.Loading -> sections(loaded, stringProvider)

            LCE.Uninitialized -> emptyList()
        }

    /** The not-found state: one localized explanation with the destination's Back affordance. */
    private fun notFound(stringProvider: StringProvider): ListModel = EmptyStateListModel(
        icon = Icon.Warning,
        explanation = stringProvider.getString(
            SampleStringId.CONTENT_DETAIL_NOT_FOUND,
        ),
    )

    private fun sections(
        content: LCE<ContentDetail>,
        stringProvider: StringProvider,
    ): List<ListModel> = listOf(
        heroSection(content, stringProvider),
        ctaSection(content),
        detailsSection(content, stringProvider),
        relatedSection(content, stringProvider),
        itemsSection(content, stringProvider),
    )

    /**
     * The hero, rendered as the destination's own artwork even while the payload is still in
     * flight.
     *
     * A detail opened from a row shares that row's seed, so the request the loading state renders
     * and the request the loaded state renders are the same [HeroImageListModel]: same [dataId],
     * same source URL, same Coil request. Two consequences fall out of that. The content detail
     * screen has its hero — and therefore its shared element — from its first composition, rather
     * than gaining one when the load finishes; and the load-to-content swap reuses the in-flight
     * (or already decoded) image instead of starting a second request.
     *
     * The only thing the loading hero cannot know is the title, so it describes itself with the
     * destination's fallback name. A state with no [contentId] (the frame before the screen's init
     * action, or a fixture that never addresses one) keeps the generic loading row.
     */
    private fun heroSection(
        content: LCE<ContentDetail>,
        stringProvider: StringProvider,
    ): ListModel {
        val addressed = contentId
        if (content is LCE.Loading && addressed != null) {
            return SectionListModel(
                dataId = SECTION_NAME_HERO.hashCode().toLong(),
                sectionItems = immutableListOf(
                    heroFor(
                        contentId = addressed,
                        contentDescription = stringProvider.getString(
                            SampleStringId.CONTENT_DETAIL_TITLE,
                        ),
                    ),
                ),
            )
        }

        return content.sectionWithStandardErrorAndLoading(
            sectionName = SECTION_NAME_HERO,
            loadingType = LoadingType.BIG_IMAGE,
            loadingItemCount = 1,
            loadingWithHeader = false,
        ) {
            listOf(heroFor(contentId = data.id, contentDescription = data.title))
        }
    }

    /** The hero for [contentId]; identical while loading and once loaded, apart from its label. */
    private fun heroFor(contentId: ContentId, contentDescription: String): HeroImageListModel =
        HeroImageListModel(
            sourceInfo = heroSourceFor(contentId),
            imagePlaceholder = Icon.Album,
            contentDescription = contentDescription,
            clickAction = ContentDetailAction.HeroClicked,
        )

    private fun ctaSection(content: LCE<ContentDetail>): ListModel =
        content.sectionWithStandardErrorAndLoading(
            sectionName = SECTION_NAME_CTA,
            loadingItemCount = 0,
            loadingWithHeader = false,
        ) {
            data.actions.map { action ->
                CtaListModel(
                    icon = action.icon,
                    name = action.label,
                    clickAction = ContentDetailAction.CtaClicked(action.label),
                )
            }
        }

    // The detail rows' labels and values come from the generated [ContentDetail.details], so the
    // screen carries no fixed metadata vocabulary. A blank field is omitted; when none remain,
    // the header disappears too.
    private fun detailsSection(
        content: LCE<ContentDetail>,
        stringProvider: StringProvider,
    ): ListModel = content.sectionWithStandardErrorAndLoading(
        sectionName = SECTION_NAME_DETAILS,
        loadingItemCount = 0,
        loadingWithHeader = false,
    ) {
        val rows = data.details.mapIndexedNotNull { index, field ->
            field.toLabelValueRow(index.toLong())
        }
        if (rows.isEmpty()) {
            emptyList()
        } else {
            listOf(
                SectionHeaderListModel(
                    stringProvider.getString(
                        SampleStringId.CONTENT_DETAIL_SECTION_DETAILS,
                    ),
                ),
            ) + rows
        }
    }

    private fun ContentDetailField.toLabelValueRow(index: Long): LabelValueListModel? {
        if (label.isBlank() || value.isBlank()) return null
        return LabelValueListModel(
            dataId = ID_PREFIX_DETAILS + index,
            label = label,
            value = value,
            clickAction = SageAction.Noop,
        )
    }

    private fun relatedSection(
        content: LCE<ContentDetail>,
        stringProvider: StringProvider,
    ): ListModel = content.sectionWithStandardErrorAndLoading(
        sectionName = SECTION_NAME_RELATED,
        loadingType = LoadingType.WIDE_ITEM,
        loadingWithHeader = true,
        loadingHorizScrollable = true,
    ) {
        listOf(
            SectionHeaderListModel(
                stringProvider.getString(SampleStringId.CONTENT_DETAIL_SECTION_RELATED),
            ),
            HorizontalScrollerListModel(
                dataId = SECTION_NAME_RELATED.hashCode().toLong() + ID_PREFIX_SCROLLER,
                scrollingItems = data.related.map { related ->
                    WideItemListModel(
                        dataId = ID_PREFIX_RELATED + related.id,
                        name = related.name,
                        sourceInfo = relatedSourceFor(parentId = data.id, relatedId = related.id),
                        imagePlaceholder = Icon.Album,
                        clickAction = ContentDetailAction.RelatedClicked(
                            contentId = relatedContentId(
                                parentId = data.id,
                                relatedId = related.id,
                            ),
                            artwork = ContentArtworkSize(
                                RELATED_IMAGE_SIZE,
                                RELATED_IMAGE_SIZE,
                                corner = ArtworkCorner.Rounded(WIDE_ITEM_CORNER_RADIUS_DP),
                            ),
                        ),
                    )
                }.toImmutableList(),
            ),
        )
    }

    private fun itemsSection(
        content: LCE<ContentDetail>,
        stringProvider: StringProvider,
    ): ListModel = content.sectionWithStandardErrorAndLoading(
        sectionName = SECTION_NAME_ITEMS,
        loadingType = LoadingType.TEXT_CAPTION_IMAGE,
        loadingItemCount = ITEMS_LOADING_COUNT,
        loadingWithHeader = true,
    ) {
        listOf(
            SectionHeaderListModel(
                stringProvider.getString(SampleStringId.CONTENT_DETAIL_SECTION_ITEMS),
            ),
        ) + data.items.map { item ->
            ImageNameCaptionListModel(
                dataId = ID_PREFIX_ITEMS + item.id,
                name = item.name,
                caption = item.caption,
                sourceInfo = itemSourceFor(parentId = data.id, itemId = item.id),
                imagePlaceholder = Icon.Person,
                clickAction = ContentDetailAction.ItemClicked(
                    contentId = contentItemContentId(parentId = data.id, itemId = item.id),
                    artwork = ContentArtworkSize(
                        ITEM_IMAGE_SIZE,
                        ITEM_IMAGE_SIZE,
                        corner = ArtworkCorner.Circle,
                    ),
                ),
            )
        }
    }

    private companion object {
        const val SECTION_NAME_HERO = "content-detail.hero"
        const val SECTION_NAME_CTA = "content-detail.cta"
        const val SECTION_NAME_DETAILS = "content-detail.details"
        const val SECTION_NAME_RELATED = "content-detail.related"
        const val SECTION_NAME_ITEMS = "content-detail.items"

        const val CONTENT_DETAIL_COLUMN_WIDTH_DP = 320
        const val ITEMS_LOADING_COUNT = 8

        /** `WideItem`'s corner radius, so the detail it opens starts from the row's shape. */
        const val WIDE_ITEM_CORNER_RADIUS_DP = 8

        const val ID_PREFIX_SCROLLER = 1_000_000_000_000L
        const val ID_PREFIX_DETAILS = 2_000_000_000_000L
        const val ID_PREFIX_RELATED = 3_000_000_000_000L
        const val ID_PREFIX_ITEMS = 4_000_000_000_000L
    }
}

/**
 * The deterministic hero request for [contentId].
 *
 * The seed comes from [contentHeroImageSeed], so a detail opened from a row reuses that row's
 * artwork. The query only selects the rendered size, so the same seed renders the same art for
 * the list thumbnail and the detail hero.
 */
internal fun heroSourceFor(
    contentId: ContentId,
    imageWidth: Int = HERO_IMAGE_WIDTH,
    imageHeight: Int = HERO_IMAGE_HEIGHT,
): SourceInfo = SourceInfo(
    GeneratedImageFetcher.urlFor(
        seed = contentHeroImageSeed(contentId),
        width = imageWidth,
        height = imageHeight,
    ),
)

/** The deterministic thumbnail request for a related item. */
internal fun relatedSourceFor(
    parentId: ContentId,
    relatedId: Long,
    imageSize: Int = RELATED_IMAGE_SIZE,
): SourceInfo = SourceInfo(
    GeneratedImageFetcher.urlFor(
        seed = contentDetailRelatedImageSeed(parentId, relatedId),
        width = imageSize,
        height = imageSize,
    ),
)

/** The deterministic thumbnail request for a content part. */
internal fun itemSourceFor(
    parentId: ContentId,
    itemId: Long,
    imageSize: Int = ITEM_IMAGE_SIZE,
): SourceInfo = SourceInfo(
    GeneratedImageFetcher.urlFor(
        seed = contentDetailItemImageSeed(parentId, itemId),
        width = imageSize,
        height = imageSize,
    ),
)

/**
 * The hero's request size. It is not part of the seed; the renderer draws the cover at its 3:4
 * aspect ratio, so the source resolution stays sharp on high-density screens.
 */
private const val HERO_IMAGE_WIDTH = 528
private const val HERO_IMAGE_HEIGHT = 704

/** A related item's square thumbnail, larger than its 64 dp row height. */
private const val RELATED_IMAGE_SIZE = 128

/** A content part's square thumbnail, larger than its row thumbnail. */
private const val ITEM_IMAGE_SIZE = 128

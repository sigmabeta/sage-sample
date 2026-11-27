package net.sigmabeta.sage.sample.feature.componentlibrary

import net.sigmabeta.sage.components.ListModel
import net.sigmabeta.sage.components.TitleBarModel
import net.sigmabeta.sage.list.ListState
import net.sigmabeta.sage.sample.strings.api.SampleStringId
import net.sigmabeta.sage.sample.ui.components.models.ButtonListModel
import net.sigmabeta.sage.sample.ui.components.models.FormMessageListModel
import net.sigmabeta.sage.sample.ui.components.models.FormMessageTone
import net.sigmabeta.sage.sample.ui.components.models.TextFieldListModel
import net.sigmabeta.sage.ui.StringProvider
import net.sigmabeta.sage.ui.componentlibrary.ComponentLibraryConfig
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.ui.componentlibrary.componentLibraryExtraId
import net.sigmabeta.sage.ui.componentlibrary.componentLibraryItems

/**
 * The Component Library's content: SAGE's catalog for [config], followed by the sample's own
 * form rows under the catalog's "App rows" header.
 */
data class ComponentLibraryState(
    val config: ComponentLibraryConfig = ComponentLibraryConfig(),
) : ListState() {
    override val columnType = config.layout.columnType

    override fun title(stringProvider: StringProvider) = TitleBarModel(
        title = stringProvider.getString(SampleStringId.NAV_COMPONENT_LIBRARY),
        shouldShowBack = true,
    )

    override fun toListItems(stringProvider: StringProvider): List<ListModel> =
        componentLibraryItems(config, extraSamples = sampleFormRows(stringProvider))
}

/**
 * One of each sample-owned row from `:core:ui:components:models`, rendered through
 * `SampleRenderers.contentFor`. Their explicit ids come from `componentLibraryExtraId`, which can't
 * collide with the catalog's.
 */
internal fun sampleFormRows(stringProvider: StringProvider): List<ListModel> = listOf(
    TextFieldListModel(
        id = TEXT_FIELD_ID,
        label = stringProvider.getString(SampleStringId.LOGIN_USERNAME_LABEL),
        text = "",
    ),
    ButtonListModel(
        label = stringProvider.getString(SampleStringId.LOGIN_SUBMIT),
        clickAction = SageAction.Noop,
        dataId = BUTTON_ID,
    ),
    FormMessageListModel(
        text = stringProvider.getString(SampleStringId.LOGIN_REJECTED),
        tone = FormMessageTone.ERROR,
        dataId = ERROR_MESSAGE_ID,
    ),
    FormMessageListModel(
        text = stringProvider.getString(SampleStringId.LOGIN_DEMO_ACCOUNT_HINT),
        tone = FormMessageTone.INFO,
        dataId = INFO_MESSAGE_ID,
    ),
)

internal val TEXT_FIELD_ID = componentLibraryExtraId(0)
internal val BUTTON_ID = componentLibraryExtraId(1)
internal val ERROR_MESSAGE_ID = componentLibraryExtraId(2)
internal val INFO_MESSAGE_ID = componentLibraryExtraId(3)

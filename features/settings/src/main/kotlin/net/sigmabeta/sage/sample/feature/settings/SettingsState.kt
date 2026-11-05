package net.sigmabeta.sage.sample.feature.settings

import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.components.ListModel
import net.sigmabeta.sage.components.SingleTextListModel
import net.sigmabeta.sage.components.TitleBarModel
import net.sigmabeta.sage.list.ListState
import net.sigmabeta.sage.ui.StringProvider

/**
 * Settings' content, declared as a SAGE [ListState].
 *
 * TODO: add the real preference rows once there are preferences to expose. SAGE already ships
 * models for them (`CheckableListModel`, `DropdownSettingListModel`, `EditTextListModel`).
 *
 * Unlike the three top-level screens this one is reached from the hamburger, so its title asks
 * for a back affordance.
 */
object SettingsState : ListState() {
    override fun title(stringProvider: StringProvider) =
        TitleBarModel(
            title = stringProvider.getString(SettingsStringId.SETTINGS_TITLE),
            shouldShowBack = true,
        )

    override fun toListItems(stringProvider: StringProvider): List<ListModel> =
        listOf(
            SingleTextListModel(
                name = stringProvider.getString(SettingsStringId.SETTINGS_TODO),
                clickAction = SageAction.Noop,
                dataId = 1L,
            ),
        )
}

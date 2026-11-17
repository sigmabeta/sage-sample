package net.sigmabeta.sage.sample.ui.components.models

import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.components.ListModel

/**
 * A labelled single-line text field in a form, rendered by `TextFieldListItem`.
 *
 * SAGE's `EditTextListModel` is a self-contained inline edit (its own submit/cancel pair, no
 * masking), so a multi-field form such as Login uses this sample-owned row instead.
 *
 * The renderer keeps the typed text locally, so typing echoes on the same frame, and reports every
 * change as [TextFieldChanged] so the reducer always knows the current value. [text] seeds that
 * local text when the row first renders and again whenever [resetKey] changes: a reducer that
 * wants to replace what is typed (for example, clearing a form) sets [text] and bumps [resetKey].
 *
 * @param id identifies the field in [TextFieldChanged] / [TextFieldDone]; also this model's [dataId]
 * @param label the field's label, shown inside it while empty and above it once filled
 * @param masked when true the text is drawn as bullets and the keyboard is a password keyboard
 * @param enabled when false the field is greyed out and ignores input
 * @param submitOnDone when true the keyboard's action is Done and sends [TextFieldDone]; when false
 *   it is Next and moves focus to the following field
 */
data class TextFieldListModel(
    val id: Long,
    val label: String,
    val text: String,
    val masked: Boolean = false,
    val enabled: Boolean = true,
    val submitOnDone: Boolean = false,
    val resetKey: Int = 0,
) : ListModel() {
    override val dataId: Long = id
    override val columns = ListModel.COLUMNS_ALL
}

/** A full-width filled button, rendered by `ButtonListItem`; sends [clickAction] when [enabled]. */
data class ButtonListModel(
    val label: String,
    val clickAction: SageAction,
    val enabled: Boolean = true,
    override val dataId: Long = label.hashCode().toLong(),
) : ListModel() {
    override val columns = ListModel.COLUMNS_ALL
}

/** The tone of a [FormMessageListModel]. */
enum class FormMessageTone {
    /** Supporting text, such as a hint. */
    INFO,

    /** A failure the user should act on, drawn in the theme's error colour. */
    ERROR,
}

/** One line of text in a form, rendered by `FormMessageListItem`. */
data class FormMessageListModel(
    val text: String,
    val tone: FormMessageTone,
    override val dataId: Long = text.hashCode().toLong(),
) : ListModel() {
    override val columns = ListModel.COLUMNS_ALL
}

/** The text in the [TextFieldListModel] identified by [id] is now [text]. */
data class TextFieldChanged(val id: Long, val text: String) : SageAction()

/** The keyboard's Done action in the [TextFieldListModel] identified by [id]. */
data class TextFieldDone(val id: Long) : SageAction()

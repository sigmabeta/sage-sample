package net.sigmabeta.sage.sample.feature.login

import net.sigmabeta.sage.components.ListModel
import net.sigmabeta.sage.components.TitleBarModel
import net.sigmabeta.sage.list.ListState
import net.sigmabeta.sage.sample.strings.api.SampleStringId
import net.sigmabeta.sage.sample.ui.components.models.ButtonListModel
import net.sigmabeta.sage.sample.ui.components.models.FormMessageListModel
import net.sigmabeta.sage.sample.ui.components.models.FormMessageTone
import net.sigmabeta.sage.sample.ui.components.models.TextFieldListModel
import net.sigmabeta.sage.ui.StringProvider

/** The list id, and the [TextFieldListModel.id], of the username field. */
const val USERNAME_FIELD_ID = 1L

/** The list id, and the [TextFieldListModel.id], of the password field. */
const val PASSWORD_FIELD_ID = 2L

/** Where the Login form is in its current Sign in attempt. */
enum class LoginStatus {
    /** Editing; no attempt in flight and no failure to report. */
    IDLE,

    /** An attempt is in flight: the form is locked until it answers. */
    SUBMITTING,

    /** Wrong username or password. */
    REJECTED,

    /** Too many recent failures; the identity source asks the client to wait. */
    THROTTLED,

    /** The identity source did not answer. */
    UNREACHABLE,
}

/**
 * The Login screen, declared as a SAGE [ListState]: a username field, a masked password field,
 * the outcome of the last failed attempt, the Sign in button, and the Demo account hint.
 *
 * [username] and [password] are the text the renderer last reported; Sign in is enabled only when
 * both are non-empty and no attempt is in flight. Neither is trimmed: the identity source matches
 * them exactly. [formKey] is passed to both fields as their reset key, so bumping it replaces
 * whatever is typed with [username] / [password].
 */
data class LoginState(
    val username: String = "",
    val password: String = "",
    val status: LoginStatus = LoginStatus.IDLE,
    val formKey: Int = 0,
) : ListState() {
    /** Whether Sign in would start an attempt now. */
    val canSubmit: Boolean
        get() = username.isNotEmpty() && password.isNotEmpty() && status != LoginStatus.SUBMITTING

    override fun title(stringProvider: StringProvider) =
        TitleBarModel(
            title = stringProvider.getString(SampleStringId.LOGIN_TITLE),
            shouldShowBack = false,
        )

    override fun toListItems(stringProvider: StringProvider): List<ListModel> {
        val editable = status != LoginStatus.SUBMITTING

        return buildList {
            add(
                TextFieldListModel(
                    id = USERNAME_FIELD_ID,
                    label = stringProvider.getString(SampleStringId.LOGIN_USERNAME_LABEL),
                    text = username,
                    enabled = editable,
                    resetKey = formKey,
                ),
            )
            add(
                TextFieldListModel(
                    id = PASSWORD_FIELD_ID,
                    label = stringProvider.getString(SampleStringId.LOGIN_PASSWORD_LABEL),
                    text = password,
                    masked = true,
                    enabled = editable,
                    submitOnDone = true,
                    resetKey = formKey,
                ),
            )

            failureMessage()?.let { id ->
                add(FormMessageListModel(stringProvider.getString(id), FormMessageTone.ERROR))
            }

            add(
                ButtonListModel(
                    label = stringProvider.getString(
                        if (editable) SampleStringId.LOGIN_SUBMIT else SampleStringId.LOGIN_SUBMITTING,
                    ),
                    clickAction = LoginAction.SubmitClicked,
                    enabled = canSubmit,
                    dataId = SUBMIT_BUTTON_ID,
                ),
            )
            add(
                FormMessageListModel(
                    text = stringProvider.getString(SampleStringId.LOGIN_DEMO_ACCOUNT_HINT),
                    tone = FormMessageTone.INFO,
                    dataId = DEMO_ACCOUNT_HINT_ID,
                ),
            )
        }
    }

    private fun failureMessage(): SampleStringId? = when (status) {
        LoginStatus.IDLE,
        LoginStatus.SUBMITTING -> null

        LoginStatus.REJECTED -> SampleStringId.LOGIN_REJECTED

        LoginStatus.THROTTLED -> SampleStringId.LOGIN_THROTTLED

        LoginStatus.UNREACHABLE -> SampleStringId.LOGIN_UNREACHABLE
    }

    private companion object {
        /** Stable list ids, so the button keeps its slot while its label changes. */
        const val SUBMIT_BUTTON_ID = 3L
        const val DEMO_ACCOUNT_HINT_ID = 4L
    }
}

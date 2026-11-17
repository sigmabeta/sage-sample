package net.sigmabeta.sage.sample.feature.login

import net.sigmabeta.sage.appcomm.SageAction

/**
 * Actions the Login rows carry. Typing arrives as the shared renderer's `TextFieldChanged`, and
 * the password field's keyboard Done as `TextFieldDone`; the button is feature vocabulary.
 */
sealed class LoginAction : SageAction() {
    /** The Sign in button. */
    data object SubmitClicked : LoginAction()
}

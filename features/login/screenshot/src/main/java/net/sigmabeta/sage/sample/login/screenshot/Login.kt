package net.sigmabeta.sage.sample.login.screenshot

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import net.sigmabeta.sage.list.WidthClass
import net.sigmabeta.sage.sample.feature.login.LoginState
import net.sigmabeta.sage.sample.feature.login.LoginStatus
import net.sigmabeta.sage.sample.ui.previews.DevicePreviews
import net.sigmabeta.sage.sample.ui.previews.ListScreenPreview
import net.sigmabeta.sage.sample.ui.previews.previewWidthClass

/** Login: empty Login form. */
@DevicePreviews
@Composable
internal fun LoginEmpty(
    darkTheme: Boolean = isSystemInDarkTheme(),
    syntheticWidthClass: WidthClass = previewWidthClass(),
) {
    ListScreenPreview(
        screenState = LoginState(),
        darkTheme = darkTheme,
        syntheticWidthClass = syntheticWidthClass,
    )
}

/** Login: both fields filled; the password is masked. */
@DevicePreviews
@Composable
internal fun LoginFilled(
    darkTheme: Boolean = isSystemInDarkTheme(),
    syntheticWidthClass: WidthClass = previewWidthClass(),
) {
    ListScreenPreview(
        screenState = filledState(),
        darkTheme = darkTheme,
        syntheticWidthClass = syntheticWidthClass,
    )
}

/** Login: an attempt in flight: form locked, button relabelled. */
@DevicePreviews
@Composable
internal fun LoginSubmitting(
    darkTheme: Boolean = isSystemInDarkTheme(),
    syntheticWidthClass: WidthClass = previewWidthClass(),
) {
    ListScreenPreview(
        screenState = filledState(LoginStatus.SUBMITTING),
        darkTheme = darkTheme,
        syntheticWidthClass = syntheticWidthClass,
    )
}

/** Login: wrong username or password. */
@DevicePreviews
@Composable
internal fun LoginRejected(
    darkTheme: Boolean = isSystemInDarkTheme(),
    syntheticWidthClass: WidthClass = previewWidthClass(),
) {
    ListScreenPreview(
        screenState = filledState(LoginStatus.REJECTED),
        darkTheme = darkTheme,
        syntheticWidthClass = syntheticWidthClass,
    )
}

/** Login: too many attempts. */
@DevicePreviews
@Composable
internal fun LoginThrottled(
    darkTheme: Boolean = isSystemInDarkTheme(),
    syntheticWidthClass: WidthClass = previewWidthClass(),
) {
    ListScreenPreview(
        screenState = filledState(LoginStatus.THROTTLED),
        darkTheme = darkTheme,
        syntheticWidthClass = syntheticWidthClass,
    )
}

/** Login: the identity source did not answer. */
@DevicePreviews
@Composable
internal fun LoginUnreachable(
    darkTheme: Boolean = isSystemInDarkTheme(),
    syntheticWidthClass: WidthClass = previewWidthClass(),
) {
    ListScreenPreview(
        screenState = filledState(LoginStatus.UNREACHABLE),
        darkTheme = darkTheme,
        syntheticWidthClass = syntheticWidthClass,
    )
}

/**
 * The form with a Demo account typed in. The credentials are the published Demo account, the same
 * placeholder text the hint under the form shows.
 */
private fun filledState(status: LoginStatus = LoginStatus.IDLE): LoginState = LoginState(
    username = "demo",
    password = "sage",
    status = status,
)

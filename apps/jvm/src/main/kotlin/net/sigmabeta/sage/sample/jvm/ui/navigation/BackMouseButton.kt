package net.sigmabeta.sage.sample.jvm.ui.navigation

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.isBackPressed
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Routes a press of the mouse "back" side-button to [onBack].
 *
 * Desktop only, because Compose desktop surfaces that button through
 * `PointerButtons.isBackPressed`; Android's `ViewRootImpl` already turns it into `KEYCODE_BACK`.
 * Button state is only read, never consumed, so primary/secondary clicks are untouched. The
 * rising-edge guard fires [onBack] once per physical press instead of repeating while held.
 */
fun Modifier.backMouseButton(onBack: () -> Unit): Modifier = pointerInput(onBack) {
    awaitPointerEventScope {
        var backHeld = false
        while (true) {
            val backPressed = awaitPointerEvent().buttons.isBackPressed
            if (backPressed && !backHeld) {
                onBack()
            }
            backHeld = backPressed
        }
    }
}

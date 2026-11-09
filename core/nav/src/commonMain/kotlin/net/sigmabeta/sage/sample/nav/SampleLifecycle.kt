package net.sigmabeta.sage.sample.nav

import androidx.lifecycle.Lifecycle
import net.sigmabeta.sage.appcomm.SageAction

/**
 * The single lifecycle-event → foreground-action decision.
 *
 * Only `ON_RESUME` and `ON_PAUSE` are foreground transitions; every other event is not, and
 * returns `null`.
 */
fun foregroundActionFor(event: Lifecycle.Event): SageAction? = when (event) {
    Lifecycle.Event.ON_RESUME -> SageAction.Resume
    Lifecycle.Event.ON_PAUSE -> SageAction.Pause
    else -> null
}

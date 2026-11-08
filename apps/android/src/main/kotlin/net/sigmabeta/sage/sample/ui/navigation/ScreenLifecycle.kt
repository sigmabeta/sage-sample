package net.sigmabeta.sage.sample.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LifecycleResumeEffect
import net.sigmabeta.sage.appcomm.ActionSink
import net.sigmabeta.sage.sample.nav.foregroundActionFor

/**
 * The [LifecycleOwner] screen entries observe for foreground/background, supplied by the app shell
 * from the platform's `LocalLifecycleOwner`.
 *
 * Defaults to `null` so a screen rendered outside the shell — an isolated screen test, a
 * `@Preview` — receives no lifecycle actions instead of crashing on a missing owner.
 */
val LocalScreenLifecycleOwner = staticCompositionLocalOf<LifecycleOwner?> { null }

/**
 * Feeds the screen's foreground lifecycle into [actionSink] (the screen's view model) as plain
 * actions:
 *
 * - `ON_RESUME` — including app foregrounding, and entering composition while already resumed —
 *   sends [SageAction.Resume].
 * - `ON_PAUSE` and disposal send [SageAction.Pause].
 *
 * Only the active (composed) screen runs this effect, so only it receives the pair. Repeated
 * notifications are coalesced in `SampleListViewModel.sendAction`, which stays the single dedupe
 * point (SAGE's list screens also send `Resume` on title changes).
 *
 * No-ops when the shell provided no [LocalScreenLifecycleOwner].
 */
@Composable
fun ScreenLifecycleEffect(actionSink: ActionSink) {
    val owner = LocalScreenLifecycleOwner.current ?: return

    LifecycleResumeEffect(owner, lifecycleOwner = owner) {
        foregroundActionFor(Lifecycle.Event.ON_RESUME)?.let(actionSink::sendAction)
        onPauseOrDispose {
            foregroundActionFor(Lifecycle.Event.ON_PAUSE)?.let(actionSink::sendAction)
        }
    }
}

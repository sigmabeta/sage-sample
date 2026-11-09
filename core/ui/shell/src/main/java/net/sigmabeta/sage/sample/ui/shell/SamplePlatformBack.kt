package net.sigmabeta.sage.sample.ui.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/**
 * The platform's whole-window Back input.
 *
 * The shell calls [Handler] only while it owns Back at the current navigation level ([enabled]),
 * so a platform that already gives pushed screens their own Back handling (Android's Voyager
 * dispatcher) is not double-popped. Desktop implementations may ignore `enabled`: nothing else
 * pops a screen there, and the shared policy itself decides Pop / ShowTopLevel / Outer.
 *
 * [wrap] contributes any window-level input that is not a lifecycle callback — the desktop mouse
 * back button is a pointer gesture, so it needs a modifier around the shell rather than a handler.
 */
interface SamplePlatformBack {
    @Composable
    fun Handler(enabled: Boolean, onBack: () -> Unit)

    /** Adds platform Back input to [base]; the default adds nothing. */
    fun wrap(base: Modifier, onBack: () -> Unit): Modifier = base
}

/** The no-op implementation used by previews and isolated renders. */
object NoopSamplePlatformBack : SamplePlatformBack {
    @Composable
    override fun Handler(enabled: Boolean, onBack: () -> Unit) = Unit
}

/** Provided by `SampleApp`; the shell reads it for both the handler and the gesture wrapper. */
val LocalSamplePlatformBack = staticCompositionLocalOf<SamplePlatformBack> {
    NoopSamplePlatformBack
}

/** Routes the platform's Back input to the shell's single Back policy. */
@Composable
internal fun SampleSystemBack(enabled: Boolean, onBack: () -> Unit) {
    LocalSamplePlatformBack.current.Handler(enabled = enabled, onBack = onBack)
}

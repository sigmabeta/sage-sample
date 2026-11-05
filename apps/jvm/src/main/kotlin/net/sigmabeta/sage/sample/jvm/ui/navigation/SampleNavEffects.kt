package net.sigmabeta.sage.sample.jvm.ui.navigation

import androidx.compose.runtime.staticCompositionLocalOf
import net.sigmabeta.sage.appcomm.SageEvent

/**
 * The app's one-shot navigation handler.
 *
 * Features stay platform-neutral: they emit SAGE [SageEvent]s (for example
 * `SageEvent.NavigateTo`) and never touch Voyager. Screens forward their view model's
 * [net.sigmabeta.sage.sample.viewmodel.SampleListViewModel.uiEvents] here, and
 * [SampleNavHost] decides what each event means for the back stack.
 */
fun interface SampleNavEffects {
    fun onEvent(event: SageEvent)
}

/** Provided by [SampleNavHost] around the current screen; there is no sensible default. */
val LocalSampleNavEffects = staticCompositionLocalOf<SampleNavEffects> {
    error("No SampleNavEffects provided — render screens inside SampleNavHost.")
}

package net.sigmabeta.sage.sample.ui.shell

import androidx.compose.runtime.staticCompositionLocalOf
import net.sigmabeta.sage.appcomm.SageEvent

/**
 * The app's one-shot effect handler.
 *
 * Features stay platform-neutral: they emit SAGE [SageEvent]s (for example
 * `SageEvent.NavigateTo` or `SageEvent.ShowSnackbar`) and never touch Voyager or Compose.
 * Screens forward their view model's
 * [net.sigmabeta.sage.sample.viewmodel.SampleListViewModel.uiEvents] here, and
 * [SampleNavHost] decides what each event means for the back stack or the snackbar host.
 */
fun interface SampleNavEffects {
    fun onEvent(event: SageEvent)
}

/** Provided by [SampleNavHost] around the current screen; there is no sensible default. */
val LocalSampleNavEffects = staticCompositionLocalOf<SampleNavEffects> {
    error("No SampleNavEffects provided — render screens inside SampleNavHost.")
}

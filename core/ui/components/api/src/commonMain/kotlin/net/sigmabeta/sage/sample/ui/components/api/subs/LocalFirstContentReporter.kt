package net.sigmabeta.sage.sample.ui.components.api.subs

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Invoked when the first content-bearing image composes — the app showing its first content.
 *
 * Hosts that measure startup provide a lambda that reports the app fully drawn (Android's
 * `Activity.reportFullyDrawn`), which the macrobenchmark's `StartupTimingMetric` records as
 * time-to-first-content. Every other host leaves it null, so this costs a null check per image in
 * production.
 *
 * Provided near the top of the Compose tree by the host app; read by `CrossfadeImage`.
 */
val LocalFirstContentReporter = staticCompositionLocalOf<(() -> Unit)?> { null }

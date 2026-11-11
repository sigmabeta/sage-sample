package net.sigmabeta.sage.sample.ui.components.api

import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.math.roundToInt

/**
 * How much slower every screen-level animation runs.
 *
 * A multiplied duration rather than a fixed one, so the whole app's motion can be slowed or sped
 * up from a single value — and so a settings-driven value can replace it later without touching the
 * call sites: they read [LocalAnimationScale], and whatever hosts the UI provides it.
 *
 * Two is deliberate rather than neutral: it is the exaggerated setting the transitions were tuned
 * against while they were being prototyped.
 */
const val DEFAULT_ANIMATION_SCALE: Float = 3f

/**
 * The scale animations currently run at. Provided by the app shell; renderers read it where they
 * declare a duration instead of naming one.
 */
val LocalAnimationScale = staticCompositionLocalOf { DEFAULT_ANIMATION_SCALE }

/** Base durations, before [LocalAnimationScale] stretches them. Shared so every fade keeps time. */
const val BASE_FADE_IN_MILLIS: Int = 220
const val BASE_FADE_OUT_MILLIS: Int = 140

/**
 * [baseMillis] stretched by [scale], never shorter than a frame's worth of time.
 *
 * Kept separate from the specs so a caller can see both the rhythm it picks and the fact that the
 * scale applies to it.
 */
fun scaledMillis(baseMillis: Int, scale: Float): Int =
    (baseMillis * scale).roundToInt().coerceAtLeast(1)

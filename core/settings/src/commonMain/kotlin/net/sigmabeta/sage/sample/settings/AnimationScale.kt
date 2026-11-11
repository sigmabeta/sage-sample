package net.sigmabeta.sage.sample.settings

/**
 * How much slower the app's screen-level animation runs.
 *
 * A multiplier rather than a duration: the renderers stretch every base time by it, so one value
 * governs fades, slides and shared-element moves together. [ONE_X] (the default) runs them at their
 * authored speed; the slower entries keep the exaggerated setting the transitions were tuned against
 * available for inspecting them.
 */
enum class AnimationScale(val multiplier: Float) {
    ONE_X(1f),
    TWO_X(2f),
    THREE_X(3f),
    FIVE_X(5f),
    ;

    companion object {
        val DEFAULT: AnimationScale = ONE_X

        /** Parses a persisted [name] back into an [AnimationScale], falling back to [DEFAULT]. */
        fun fromStorageValue(value: String?): AnimationScale =
            entries.firstOrNull { it.name == value } ?: DEFAULT
    }
}

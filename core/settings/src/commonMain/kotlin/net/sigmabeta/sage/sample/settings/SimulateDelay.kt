package net.sigmabeta.sage.sample.settings

/**
 * How long every simulated background load waits before emitting.
 *
 * The sample's demo repositories use this to make loading and error states visible; [NONE]
 * disables the delay entirely and is the default.
 */
enum class SimulateDelay(val millis: Long) {
    NONE(0L),
    ONE_SECOND(1_000L),
    TWO_SECONDS(2_000L),
    FIVE_SECONDS(5_000L),
    ;

    companion object {
        val DEFAULT: SimulateDelay = NONE

        /** Parses a persisted [name] back into a [SimulateDelay], falling back to [DEFAULT]. */
        fun fromStorageValue(value: String?): SimulateDelay =
            entries.firstOrNull { it.name == value } ?: DEFAULT
    }
}

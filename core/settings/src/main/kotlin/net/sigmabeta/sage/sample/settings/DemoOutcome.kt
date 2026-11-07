package net.sigmabeta.sage.sample.settings

/**
 * Which outcome the sample's generated demo data sources should produce.
 *
 * [CONTENT] is the ordinary generated result set and the default. [EMPTY] returns a successful
 * but empty result set, and [ERROR] makes the load fail, so the screens' terminal states are
 * reachable in the running demo without source changes. The enum is the shared vocabulary for
 * every demo selector; later screens reuse it rather than adding their own.
 */
enum class DemoOutcome {
    CONTENT,
    EMPTY,
    ERROR,
    ;

    companion object {
        val DEFAULT: DemoOutcome = CONTENT

        /** Parses a persisted [name] back into a [DemoOutcome], falling back to [DEFAULT]. */
        fun fromStorageValue(value: String?): DemoOutcome =
            entries.firstOrNull { it.name == value } ?: DEFAULT
    }
}

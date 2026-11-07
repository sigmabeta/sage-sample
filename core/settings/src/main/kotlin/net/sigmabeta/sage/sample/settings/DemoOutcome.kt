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

/**
 * Resolves this outcome to one of a generated datasource's three results.
 *
 * The four demo repositories share this so the branch set and the ERROR failure cannot drift
 * between screens. Each repository still reads the setting at call time; this only replaces the
 * repeated `when`.
 */
fun <T> DemoOutcome.select(
    content: () -> T,
    empty: () -> T,
    error: () -> T,
): T = when (this) {
    DemoOutcome.CONTENT -> content()
    DemoOutcome.EMPTY -> empty()
    DemoOutcome.ERROR -> error()
}

/** The one failure every generated datasource throws for [DemoOutcome.ERROR]. */
fun demoOutcomeFailure(): IllegalStateException =
    IllegalStateException("Demo outcome is ERROR")

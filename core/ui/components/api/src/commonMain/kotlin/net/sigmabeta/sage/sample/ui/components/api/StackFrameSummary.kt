package net.sigmabeta.sage.sample.ui.components.api

/**
 * One-line "file: line (method)" summary of this error's top stack frame, shown only in the
 * debug / inspection error overlay (see [EmptyListIndicator]).
 *
 * The sample targets Android and the JVM, both of which have a real `StackTraceElement`, so a
 * single common implementation over [Throwable.stackTraceToString] is enough.
 */
internal fun Throwable.firstFrameSummary(): String =
    stackTraceToString()
        .lineSequence()
        .drop(1)
        .firstOrNull { it.isNotBlank() }
        ?.trim()
        ?: this::class.simpleName.orEmpty()

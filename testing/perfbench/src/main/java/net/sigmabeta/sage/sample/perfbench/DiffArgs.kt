package net.sigmabeta.sage.sample.perfbench

import java.io.File

/**
 * `diff --a <baseline> --b <current> [--json]`.
 *
 * Each side is a `results-<axis>.json` file. [json] emits the machine-readable [DiffReport]
 * instead of the human table.
 */
data class DiffArgs(
    val baseline: File,
    val current: File,
    val json: Boolean,
)

/** Parses `diff`'s arguments, rejecting anything the command cannot honour. */
fun parseDiffArgs(tokens: List<String>): DiffArgs {
    val values = LinkedHashMap<String, String>()
    var i = 0
    while (i < tokens.size) {
        val token = tokens[i]
        require(token.startsWith("--")) { "Unexpected argument '$token' (options must start with --)" }
        val key = token.removePrefix("--")
        if (key == "json") {
            values[key] = "true"
            i += 1
        } else {
            val value = tokens.getOrNull(i + 1)
            require(value != null && !value.startsWith("--")) { "--$key needs a value" }
            values[key] = value
            i += 2
        }
    }
    require(values.keys.all { it in KNOWN_OPTIONS }) {
        "Unknown option(s): ${values.keys.filterNot { it in KNOWN_OPTIONS }.joinToString(", ")}"
    }
    return DiffArgs(
        baseline = File(requireNotNull(values["a"]) { "diff needs --a <baseline>" }),
        current = File(requireNotNull(values["b"]) { "diff needs --b <current>" }),
        json = values["json"] == "true",
    )
}

/** The result of reading a run's file: the decoded document, or why it is not compatible. */
internal sealed interface ResultRead {
    data class Decoded(val result: PerfResult) : ResultRead
    data class Incompatible(val file: File, val reason: String) : ResultRead
}

/** Reads a run's file, turning an undecodable result into a refusal instead of an exception. */
internal fun readResult(file: File): ResultRead =
    try {
        ResultRead.Decoded(PerfbenchJson.decodeFromString(file.readText()))
    } catch (e: Exception) {
        ResultRead.Incompatible(file, e.message ?: "unreadable perfbench result")
    }

private val KNOWN_OPTIONS = setOf("a", "b", "json")

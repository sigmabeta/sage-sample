package net.sigmabeta.sage.sample.perfbench

import java.io.File

/**
 * `baseline --runs <a.json,b.json,...> --out <baseline.json>`.
 *
 * [recordings] are one device's result files, oldest first; the newest supplies the merged
 * document's fingerprint. [outFile] is the baseline file to write.
 */
data class BaselineArgs(
    val recordings: List<File>,
    val outFile: File,
)

/** Parses `baseline`'s arguments, rejecting anything the command cannot honour. */
fun parseBaselineArgs(tokens: List<String>): BaselineArgs {
    val values = LinkedHashMap<String, String>()
    var i = 0
    while (i < tokens.size) {
        val token = tokens[i]
        require(token.startsWith("--")) { "Unexpected argument '$token' (options must start with --)" }
        val key = token.removePrefix("--")
        val value = tokens.getOrNull(i + 1)
        require(value != null && !value.startsWith("--")) { "--$key needs a value" }
        values[key] = value
        i += 2
    }
    require(values.keys.all { it in KNOWN_OPTIONS }) {
        "Unknown option(s): ${values.keys.filterNot { it in KNOWN_OPTIONS }.joinToString(", ")}"
    }
    val runs = requireNotNull(values["runs"]) { "baseline needs --runs <a.json,b.json,...> (oldest first)" }
    val out = requireNotNull(values["out"]) { "baseline needs --out <baseline.json>" }
    val recordings = runs.split(',').map { it.trim() }.filter { it.isNotEmpty() }.map(::File)
    require(recordings.isNotEmpty()) { "baseline --runs names no files" }
    recordings.forEach { require(it.isFile) { "No recording at ${it.absolutePath}" } }
    return BaselineArgs(recordings = recordings, outFile = File(out))
}

private val KNOWN_OPTIONS = setOf("runs", "out")

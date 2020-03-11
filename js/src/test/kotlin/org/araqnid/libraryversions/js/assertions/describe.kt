package org.araqnid.libraryversions.js.assertions

fun describe(v: Any?): String =
        when (v) {
            null -> "null"
            is SelfDescribing -> v.description
            is String -> "\"" + v.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
            is Pair<*, *> -> Pair(describe(v.first), describe(v.second)).toString()
            is Triple<*, *, *> -> Triple(describe(v.first), describe(v.second), describe(v.third)).toString()
            is ClosedRange<*> -> "${describe(v.start)}..${describe(v.endInclusive)}"
            is Set<*> -> v.joinToString(prefix = "{", separator = ", ", postfix = "}", transform = ::describe)
            is Collection<*> -> v.joinToString(prefix = "[", separator = ", ", postfix = "]", transform = ::describe)
            is Map<*, *> -> v.entries.joinToString(prefix = "{", separator = ", ", postfix = "}")
            { "${describe(it.key)}:${describe(it.value)}" }
            else -> v.toString()
        }

interface SelfDescribing {
    val description: String
}

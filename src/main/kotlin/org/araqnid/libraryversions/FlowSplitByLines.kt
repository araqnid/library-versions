package org.araqnid.libraryversions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

fun Flow<CharSequence>.splitByLines(separator: String = "\n"): Flow<String> {
    return flow {
        var residualPrefix: String? = null
        collect { text ->
            var pos = residualPrefix?.let { prefix ->
                val firstMatch = text.indexOf(separator)
                if (firstMatch < 0) {
                    residualPrefix += text
                    return@collect
                }
                emit(buildString(capacity = firstMatch - 0 + prefix.length) {
                    append(prefix)
                    append(text, 0, firstMatch)
                })
                firstMatch + separator.length
            } ?: 0
            while (true) {
                val nextMatch = text.indexOf(separator, startIndex = pos)
                if (nextMatch < 0)
                    break
                emit(text.substring(pos, nextMatch))
                pos = nextMatch + separator.length
            }
            residualPrefix = if (pos == text.length) null else text.substring(pos)
        }
        residualPrefix?.let { emit(it) }
    }
}

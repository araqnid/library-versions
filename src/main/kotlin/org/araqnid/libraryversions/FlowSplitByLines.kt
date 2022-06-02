package org.araqnid.libraryversions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

fun Flow<CharSequence>.splitByLines(separator: String = "\n"): Flow<String> {
    return flow {
        val residual = StringBuilder()
        collect { chunk ->
            var pos = 0
            while (true) {
                val nextLineBreak = chunk.indexOf(separator, startIndex = pos)
                if (nextLineBreak < 0) {
                    residual.append(chunk, pos, chunk.length)
                    break
                }
                if (residual.isNotEmpty()) {
                    residual.append(chunk, pos, nextLineBreak)
                    emit(residual.toString())
                    residual.clear()
                } else {
                    emit(chunk.substring(pos, nextLineBreak))
                }
                pos = nextLineBreak + separator.length
            }
        }
        if (residual.isNotEmpty())
            emit(residual.toString())
    }
}

package org.araqnid.libraryversions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset

fun Flow<ByteBuffer>.decodeText(charset: Charset = Charsets.UTF_8): Flow<CharBuffer> {
    return flow {
        val decoder = charset.newDecoder()
        collect { input ->
            val output = CharBuffer.allocate(8192)
            val result = decoder.decode(input, output, false)
            if (result.isError)
                error("invalid text: $result")
            output.flip()
            emit(output)
        }
    }
}

fun Flow<CharSequence>.splitByLines(separator: String = "\n"): Flow<String> {
    return flow {
        var residualPrefix = ""
        collect { text ->
            var pos = 0
            residualPrefix.takeIf { it.isNotEmpty() }?.let { prefix ->
                val firstMatch: Int = text.indexOf(separator)
                if (firstMatch < 0) {
                    residualPrefix += text
                    return@collect
                }
                emit(text.extractSubstringWithPrefix(prefix, 0, firstMatch))
                pos = firstMatch + separator.length
            }
            var nextMatch: Int = text.indexOf(separator, startIndex = pos)
            while (nextMatch >= 0) {
                emit(text.substring(pos, nextMatch))
                pos = nextMatch + separator.length
                residualPrefix = ""
                nextMatch = text.indexOf(separator, startIndex = pos)
            }
            residualPrefix = text.substring(pos)
        }
        if (residualPrefix.isNotEmpty())
            emit(residualPrefix)
    }
}

private fun CharSequence.extractSubstringWithPrefix(prefix: String, pos: Int, endPos: Int): String {
    return buildString(capacity = endPos - pos + prefix.length) {
        append(prefix)
        append(this@extractSubstringWithPrefix, pos, endPos)
    }
}

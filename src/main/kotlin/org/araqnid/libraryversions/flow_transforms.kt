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
            var nextMatch: Int = text.indexOf(separator, startIndex = pos)
            while (nextMatch >= 0) {
                emit(text.substring(pos, nextMatch).let { if (residualPrefix.isNotEmpty()) residualPrefix + it else it })
                pos = nextMatch + separator.length
                residualPrefix = ""
                nextMatch = text.indexOf(separator, startIndex = pos)
            }
            residualPrefix += text.substring(pos)
        }
        if (residualPrefix.isNotEmpty())
            emit(residualPrefix)
    }
}

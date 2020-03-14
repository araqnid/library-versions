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
        var residual: ByteBuffer? = null
        collect { input ->
            val output = CharBuffer.allocate(2048)
            val inputWithResidual = residual?.let { prefixBuffer ->
                ByteBuffer.allocate(prefixBuffer.remaining() + input.limit())!!
                    .put(prefixBuffer)
                    .put(input)
                    .rewind()
            } ?: input
            while (true) {
                val result = decoder.decode(inputWithResidual, output, false)
                if (result.isError)
                    error("invalid text: $result")
                output.flip()
                emit(output)
                if (result.isUnderflow) {
                    residual = if (inputWithResidual.remaining() > 0) inputWithResidual else null
                    break
                }
                output.clear()
            }
        }
        val output = CharBuffer.allocate(2048)
        val finalInput = residual ?: ByteBuffer.allocate(0)!!
        while (true) {
            val result = decoder.decode(finalInput, output, true)
            if (result.isError)
                error("invalid text: $result")
            output.flip()
            if (output.limit() > 0)
                emit(output)
            if (result.isUnderflow)
                break
        }
        output.clear()
        while (true) {
            val result = decoder.flush(output)
            if (result.isError)
                error("failed to flush decoder: $result")
            output.flip()
            if (output.limit() > 0)
                emit(output)
            if (result.isUnderflow)
                break
        }
    }
}

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


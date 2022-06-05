package org.araqnid.libraryversions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset

fun Flow<ByteBuffer>.decodeText(charset: Charset = Charsets.UTF_8, bufferSize: Int = 2048): Flow<CharBuffer> {
    return flow {
        val decoder = charset.newDecoder()
        var residual: ByteBuffer? = null
        collect { input ->
            val inputWithResidual = residual?.let { prefixBuffer ->
                ByteBuffer.allocate(prefixBuffer.remaining() + input.limit())!!
                    .put(prefixBuffer)
                    .put(input)
                    .rewind()
            } ?: input
            while (true) {
                val output = CharBuffer.allocate(bufferSize)
                val result = decoder.decode(inputWithResidual, output, false)
                if (result.isError)
                    error("invalid text: $result")
                output.flip()
                emit(output)
                if (result.isUnderflow) {
                    residual = if (inputWithResidual.remaining() > 0) inputWithResidual else null
                    break
                }
            }
        }
        val finalInput = residual ?: ByteBuffer.allocate(0)!!
        while (true) {
            val output = CharBuffer.allocate(bufferSize)
            val result = decoder.decode(finalInput, output, true)
            if (result.isError)
                error("invalid text: $result")
            output.flip()
            if (output.limit() > 0)
                emit(output)
            if (result.isUnderflow)
                break
        }
        while (true) {
            val output = CharBuffer.allocate(bufferSize)
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

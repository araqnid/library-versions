package org.araqnid.libraryversions

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.util.zip.Inflater
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalUnsignedTypes::class)
private class GzipHeaderReader {
    private var currentBuffer: ByteBuffer? = null
    private val suspended = atomic<Continuation<Unit>?>(null)

    fun addInput(buffer: ByteBuffer) {
        check(currentBuffer == null) { "currentBuffer was already set" }
        currentBuffer = buffer
        suspended.getAndSet(null)?.resume(Unit)
    }

    suspend fun read() {
        if (readUShort() != GZIP_MAGIC.toUShort()) error("Not in GZIP format")
        if (readUByte() != 8.toUByte()) error("Unsupported compression method")
        val flags = readUByte()
        skipBytes(6)
        if ((flags and FEXTRA.toUByte()) != 0.toUByte()) {
            val m = readUShort().toInt()
            skipBytes(m)
        }
        if ((flags and FNAME.toUByte()) != 0.toUByte()) {
            while (true) {
                if (readUByte() == 0.toUByte())
                    break
            }
        }
        if ((flags and FCOMMENT.toUByte()) != 0.toUByte()) {
            while (true) {
                if (readUByte() == 0.toUByte())
                    break
            }
        }
        if ((flags and FHCRC.toUByte()) != 0.toUByte()) {
            // TODO actually check CRC
            readUShort()
        }
    }

    private suspend fun readUByte(): UByte {
        while (true) {
            currentBuffer?.let { buf ->
                if (buf.remaining() > 0)
                    return buf.get().toUByte()
            }
            currentBuffer = null

            suspendCoroutine<Unit> { cont ->
                suspended.value = cont
            }
        }
    }

    private suspend fun readUShort(): UShort {
        val lo = readUByte()
        val hi = readUByte()
        return lo.toUShort() or (hi.toUInt() shl 8).toUShort()
    }

    private suspend fun skipBytes(n: Int) {
        for (i in 1..n) {
            readUByte()
        }
    }

    companion object {
        private const val GZIP_MAGIC: Int = 0x8b1f

        private const val FTEXT = 1 // Extra text
        private const val FHCRC = 2 // Header CRC
        private const val FEXTRA = 4 // Extra field
        private const val FNAME = 8 // File name
        private const val FCOMMENT = 16 // File comment
    }
}

fun Flow<ByteBuffer>.gunzip(): Flow<ByteBuffer> {
    return flow {
        val headerReader = GzipHeaderReader()
        var finishedHeader = false
        var headerParseError: Throwable? = null
        headerReader::read.startCoroutine(object : Continuation<Unit> {
            override val context: CoroutineContext
                get() = EmptyCoroutineContext

            override fun resumeWith(result: Result<Unit>) {
                if (result.isSuccess)
                    finishedHeader = true
                else {
                    headerParseError = result.exceptionOrNull()
                }
            }
        })
        val inflater = Inflater(true)
        try {
            collect { buffer ->
                if (!finishedHeader) {
                    headerReader.addInput(buffer)
                    headerParseError?.let { throw it }
                    if (!finishedHeader)
                        return@collect
                }
                inflater.setInput(buffer)
                produce@
                while (true) {
                    val output = ByteBuffer.allocate(2048)
                    val bytesProduced = inflater.inflate(output)
                    if (bytesProduced != 0) {
                        output.flip()
                        emit(output)
                    }
                    else {
                        break@produce
                    }
                }
            }
        } finally {
            inflater.end()
        }
    }
}

fun Flow<ByteBuffer>.decodeText(charset: Charset = Charsets.UTF_8): Flow<CharBuffer> {
    return flow {
        val decoder = charset.newDecoder()
        collect { input ->
            val output = CharBuffer.allocate(8192)
            val result = decoder.decode(input, output, false)
            if (result.isError)
                error("invalid text: $result")
            emit(output)
        }
    }
}

fun Flow<CharSequence>.splitByLines(): Flow<String> {
    return flow {
        var residualPrefix = ""
        collect { text ->
            var pos = 0
            var nextMatch: Int = text.indexOf("\n", startIndex = pos)
            while (nextMatch >= 0) {
                emit(residualPrefix + text.substring(pos, nextMatch))
                pos = nextMatch + 1
                residualPrefix = ""
                nextMatch = text.indexOf("\n", startIndex = pos)
            }
            residualPrefix = text.substring(pos)
        }
    }
}

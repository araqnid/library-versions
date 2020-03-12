package org.araqnid.libraryversions

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import java.nio.ByteBuffer
import java.util.zip.CRC32
import java.util.zip.Inflater
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine

fun Flow<ByteBuffer>.gunzip(): Flow<ByteBuffer> {
    return flow {
        val headerReader = GzipHeaderReader()
        var headerFinished = false
        var headerError: Throwable? = null
        headerReader::read.startCoroutine(object : Continuation<Unit> {
            override val context = EmptyCoroutineContext

            override fun resumeWith(result: Result<Unit>) {
                result.fold(
                        onSuccess = { headerFinished = true },
                        onFailure = { headerError = it }
                )
            }
        })
        val inflaterReader = GzipInflaterReader()
        var inflaterResidual: ByteBuffer? = null
        var inflaterError: Throwable? = null
        var inflaterProduced: Int = 0
        inflaterReader::read.startCoroutine(object : Continuation<ByteBuffer> {
            override val context = EmptyCoroutineContext

            override fun resumeWith(result: Result<ByteBuffer>) {
                result.fold(
                        onSuccess = { inflaterResidual = it },
                        onFailure = { inflaterError = it }
                )
            }
        })
        val trailerReader = GzipTrailerReader()
        var trailerProduced: GzipTrailerReader.GzipTrailer? = null
        var trailerError: Throwable? = null
        trailerReader::read.startCoroutine(object : Continuation<GzipTrailerReader.GzipTrailer> {
            override val context = EmptyCoroutineContext

            override fun resumeWith(result: Result<GzipTrailerReader.GzipTrailer>) {
                result.fold(
                        onSuccess = { trailerProduced = it },
                        onFailure = { trailerError = it }
                )
            }
        })
        fun checkTrailer(trailer: GzipTrailerReader.GzipTrailer) {
            if (inflaterReader.crc.value != trailer.theirCrc)
                error("CRC error in gunzipped content; ourCrc=0x${inflaterReader.crc.value.toString(radix=16)} theirCrc=${trailer.theirCrc.toString(radix=16)}")
            if (inflaterProduced != trailer.size)
                error("Length differs in gunzipped content; ourSize=$inflaterProduced theirSize=${trailer.size}")
        }
        collect { buffer ->
            if (!headerFinished) {
                headerReader.addInput(buffer)
                headerError?.let { throw it }
                if (!headerFinished)
                    return@collect
            }
            if (inflaterResidual == null) {
                inflaterReader.addInput(buffer)
                while (true) {
                    val inflaterOutput = inflaterReader.pollOutput() ?: break
                    inflaterProduced += inflaterOutput.limit()
                    emit(inflaterOutput)
                }
                inflaterError?.let { throw it }
                if (inflaterResidual == null) {
                    return@collect
                }
                trailerReader.addInput(inflaterResidual!!)
                trailerError?.let { throw it }
                trailerProduced?.let { trailer ->
                    checkTrailer(trailer)
                }
                return@collect
            }
            trailerReader.addInput(buffer)
            trailerError?.let { throw it }
            trailerProduced?.let { trailer ->
                checkTrailer(trailer)
            }
        }
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
private class GzipHeaderReader : BufferParser<Unit>() {
    private val crc = CRC32()

    override suspend fun read() {
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
            val theirCrc = (crc.value and 0xffff).toUShort()
            val ourCrc = readUShort()
            check(theirCrc == ourCrc) { "GZIP header CRC mismatch: ours=0x${ourCrc.toString(radix=16)} theirs=0x${theirCrc.toString(radix = 16)}"}
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

private class GzipInflaterReader : BufferParser<ByteBuffer>() {
    val crc = CRC32()
    private val emitting = atomic<Pair<ByteBuffer, Continuation<Unit>>?>(null)

    fun pollOutput(): ByteBuffer? {
        val ready = emitting.getAndSet(null) ?: return null
        ready.second.resume(Unit)
        return ready.first
    }

    override suspend fun read(): ByteBuffer {
        val inflater = Inflater(true)
        try {
            while (true) {
                val buffer = nextBuffer()
                inflater.setInput(buffer)
                produce@
                while (true) {
                    val output = ByteBuffer.allocate(2048)
                    val bytesProduced = inflater.inflate(output)
                    if (bytesProduced != 0) {
                        output.flip()
                        crc.update(output.asReadOnlyBuffer())
                        emit(output)
                    }
                    else if (inflater.finished()) {
                        return buffer
                    }
                    else if (inflater.needsDictionary()) {
                        throw UnsupportedOperationException("inflater needs dictionary -- not implemented")
                    }
                    else if (inflater.needsInput()) {
                        break@produce
                    }
                    else {
                        error("inflater did not produce output, has not finished but does not need input - ???")
                    }
                }
            }
        } finally {
            inflater.end()
        }
    }

    private suspend fun emit(buffer: ByteBuffer) {
        suspendCoroutine<Unit> { cont ->
            if (!emitting.compareAndSet(null, buffer to cont))
                error("was already emitting when emit() was called")
        }
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
private class GzipTrailerReader : BufferParser<GzipTrailerReader.GzipTrailer>() {
    data class GzipTrailer(val theirCrc: Long, val size: Int)

    override suspend fun read(): GzipTrailer {
        val theirCrc = readUInt()
        val size = readUInt()
        return GzipTrailer(theirCrc.toLong(), size.toInt())
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
private abstract class BufferParser<T> {
    private val currentBuffer = atomic<ByteBuffer?>(null)
    private val suspended = atomic<Continuation<Unit>?>(null)

    fun addInput(buffer: ByteBuffer) {
        if (!currentBuffer.compareAndSet(null, buffer))
            error("currentBuffer was already set")
        suspended.getAndSet(null)?.resume(Unit)
    }

    abstract suspend fun read(): T

    protected suspend fun nextBuffer(): ByteBuffer {
        while (true) {
            val alreadyGot = currentBuffer.getAndSet(null)
            if (alreadyGot != null)
                return alreadyGot
            suspendCoroutine<Unit> { cont ->
                if (!suspended.compareAndSet(null, cont))
                    error("was already suspended when nextBuffer() was called")
            }
        }
    }

    protected suspend fun readUByte(): UByte {
        while (true) {
            currentBuffer.value?.let { buf ->
                if (buf.remaining() > 0)
                    return buf.get().toUByte()
            }
            currentBuffer.value = null

            suspendCoroutine<Unit> { cont ->
                if (!suspended.compareAndSet(null, cont))
                    error("was already suspended when readUByte() was called")
            }
        }
    }

    protected suspend fun readUShort(): UShort {
        val lo = readUByte()
        val hi = readUByte()
        return lo.toUShort() or (hi.toUInt() shl 8).toUShort()
    }

    protected suspend fun readUInt(): UInt {
        val lo = readUShort()
        val hi = readUShort()
        return lo.toUInt() or (hi.toULong() shl 16).toUInt()
    }

    protected suspend fun skipBytes(n: Int) {
        for (i in 1..n) {
            readUByte()
        }
    }
}

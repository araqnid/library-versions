package org.araqnid.libraryversions

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import java.nio.ByteBuffer

/**
 * Read a flow of byte buffers, providing access to the overall byte stream either buffer-by-buffer
 * **or** byte-by-byte. This facilities parsing structured messages from the content while also
 * allowing for passing through streams of data to a compression codec, for example.
 *
 * @param reader Use the reading methods on BufferReaderScope and call `emit` to produce output
 * @return transformed flow
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun <O> Flow<ByteBuffer>.readBuffers(reader: suspend BufferReaderScope<O>.() -> Unit): Flow<O> {
    return flow {
        coroutineScope {
            produce {
                collect { send(it) }
            }.consume {
                BufferReaderScopeImpl<O>(this, this@flow).reader()
            }
        }
    }
}

/**
 * Scope for reading stream of buffers in either byte-by-byte or buffer-by-buffer mode.
 */
interface BufferReaderScope<in O> : FlowCollector<O> {
    /**
     * Start or continue reading buffer-by-buffer. The buffer returned may already have a non-zero position when
     * switching from reading byte-by-byte.
     */
    suspend fun nextBuffer(): ByteBuffer

    /**
     * After reading buffer-by-buffer, put a partially-consumed buffer back into the scope for subsequent
     * byte-by-byte reading. Byte reads will start from the buffer's position. If the buffer's position is
     * already at the limit, the buffer will be ignored.
     */
    fun putBackBuffer(buffer: ByteBuffer)

    /**
     * Read next byte. This is the fundamental operation for reading byte-by-byte.
     */
    suspend fun readUByte(): Int

    /**
     * Read 16-bit value in little-endian byte order.
     */
    suspend fun readUShort(): Int {
        val lo = readUByte()
        val hi = readUByte()
        return lo or (hi shl 8)
    }

    /**
     * Read 32-bit value in little-endian byte order.
     */
    suspend fun readUInt(): Int {
        val lo = readUShort()
        val hi = readUShort()
        return lo or (hi shl 16)
    }

    /**
     * Skip a fixed number of bytes.
     */
    suspend fun skipBytes(n: Int) {
        for (i in 1..n) {
            readUByte()
        }
    }
}

private class BufferReaderScopeImpl<in O>(private val input: ReceiveChannel<ByteBuffer>, collector: FlowCollector<O>) :
    BufferReaderScope<O>,
    FlowCollector<O> by collector {
    @Volatile
    private var currentBuffer: ByteBuffer? = null

    override suspend fun nextBuffer(): ByteBuffer {
        val buffer = currentBuffer
        if (buffer != null) {
            currentBuffer = null
            return buffer
        }
        return input.receive()
    }

    override fun putBackBuffer(buffer: ByteBuffer) {
        check(currentBuffer == null) { "had already pulled a buffer when putBackBuffer() was called" }
        currentBuffer = buffer
    }

    override suspend fun readUByte(): Int = buffer().get().toInt() and 0xff

    private suspend fun buffer(): ByteBuffer {
        while (true) {
            val buffer = currentBuffer
            if (buffer != null && buffer.hasRemaining())
                return buffer
            currentBuffer = input.receive()
        }
    }
}

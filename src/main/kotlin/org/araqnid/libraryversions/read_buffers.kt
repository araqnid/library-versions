package org.araqnid.libraryversions

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.updateAndGet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import java.nio.ByteBuffer

@OptIn(ExperimentalCoroutinesApi::class)
fun <O> Flow<ByteBuffer>.readBuffers(reader: suspend BufferReaderScope<O>.() -> Unit): Flow<O> {
    return flow {
        coroutineScope {
            val buffersInputChannel = produce(capacity = Channel.RENDEZVOUS) {
                collect { send(it) }
            }

            BufferReaderScopeImpl<O>(buffersInputChannel, this@flow).reader()
        }
    }
}

interface BufferReaderScope<in O> : FlowCollector<O> {
    suspend fun nextBuffer(): ByteBuffer
    fun putBackBuffer(buffer: ByteBuffer)

    suspend fun readUByte(): Int

    suspend fun readUShort(): Int {
        val lo = readUByte()
        val hi = readUByte()
        return lo or (hi shl 8)
    }

    suspend fun readUInt(): Int {
        val lo = readUShort()
        val hi = readUShort()
        return lo or (hi shl 16)
    }

    suspend fun skipBytes(n: Int) {
        for (i in 1..n) {
            readUByte()
        }
    }
}

private class BufferReaderScopeImpl<in O>(private val input: ReceiveChannel<ByteBuffer>, collector: FlowCollector<O>) :
        BufferReaderScope<O>,
        FlowCollector<O> by collector {
    private val currentBuffer = atomic<ByteBuffer?>(null)

    override suspend fun nextBuffer(): ByteBuffer {
        currentBuffer.getAndSet(null)?.let { return it }
        return input.receive()
    }

    override fun putBackBuffer(buffer: ByteBuffer) {
        if (!currentBuffer.compareAndSet(null, buffer))
            error("had already pulled a buffer when putBackBuffer() was called")
    }

    override suspend fun readUByte(): Int = buffer().get().toInt() and 0xff

    private suspend fun buffer(): ByteBuffer {
        while (true) {
            val buf = currentBuffer.updateAndGet { maybeBuf ->
                if (maybeBuf != null && maybeBuf.hasRemaining()) maybeBuf else null
            }
            if (buf != null) return buf
            val next = nextBuffer()
            currentBuffer.value = next
        }
    }
}

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

@OptIn(ExperimentalUnsignedTypes::class)
interface BufferReaderScope<in O> : FlowCollector<O> {
    suspend fun nextBuffer(): ByteBuffer
    fun putBackBuffer(buffer: ByteBuffer)

    suspend fun readUByte(): UByte

    suspend fun readUShort(): UShort {
        val lo = readUByte()
        val hi = readUByte()
        return lo.toUShort() or (hi.toUInt() shl 8).toUShort()
    }

    suspend fun readUInt(): UInt {
        val lo = readUShort()
        val hi = readUShort()
        return lo.toUInt() or (hi.toULong() shl 16).toUInt()
    }

    suspend fun skipBytes(n: Int) {
        for (i in 1..n) {
            readUByte()
        }
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
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

    override suspend fun readUByte(): UByte = buffer().get().toUByte()

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

package org.araqnid.libraryversions

import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import java.nio.ByteBuffer
import java.util.zip.CRC32
import java.util.zip.Inflater

fun Flow<ByteBuffer>.gunzip(): Flow<ByteBuffer> {
    return readBuffers { destination ->
        try {
            readGzipHeader()
            val crc = CRC32()
            val inflaterProduced = inflate(crc, destination)
            val trailer = readGzipTrailer()
            if (crc.value != trailer.theirCrc)
                error("CRC error in gunzipped content; ourCrc=0x${crc.value.toString(radix=16)} theirCrc=${trailer.theirCrc.toString(radix=16)}")
            if (inflaterProduced != trailer.size)
                error("Length differs in gunzipped content; ourSize=$inflaterProduced theirSize=${trailer.size}")
        } catch (e: ClosedReceiveChannelException) {
            throw IllegalStateException("Truncated GZIP input", e)
        }
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
private suspend fun BufferReaderScope.readGzipHeader() {
    val crc = CRC32()
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

private suspend fun BufferReaderScope.inflate(crc: CRC32, destination: SendChannel<ByteBuffer>): Int {
    val inflater = Inflater(true)
    var totalBytes = 0
    try {
        while (true) {
            val buffer = nextBuffer()
            inflater.setInput(buffer)
            produce@
            while (true) {
                val output = ByteBuffer.allocate(2048)
                val bytesProduced = inflater.inflate(output)
                if (bytesProduced != 0) {
                    totalBytes += bytesProduced
                    output.flip()
                    crc.update(output.asReadOnlyBuffer())
                    destination.send(output)
                }
                else if (inflater.finished()) {
                    putBackBuffer(buffer)
                    return totalBytes
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

@OptIn(ExperimentalUnsignedTypes::class)
private suspend fun BufferReaderScope.readGzipTrailer(): GzipTrailer {
    val theirCrc = readUInt()
    val size = readUInt()
    return GzipTrailer(theirCrc.toLong(), size.toInt())
}

private data class GzipTrailer(val theirCrc: Long, val size: Int)

private const val GZIP_MAGIC: Int = 0x8b1f

private const val FTEXT = 1 // Extra text
private const val FHCRC = 2 // Header CRC
private const val FEXTRA = 4 // Extra field
private const val FNAME = 8 // File name
private const val FCOMMENT = 16 // File comment

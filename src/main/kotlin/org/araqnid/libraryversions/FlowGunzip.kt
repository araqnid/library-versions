package org.araqnid.libraryversions

import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.Flow
import java.nio.ByteBuffer
import java.util.zip.CRC32
import java.util.zip.Inflater

fun Flow<ByteBuffer>.gunzip(): Flow<ByteBuffer> {
    return readBuffers {
        try {
            readGzipHeader()
            val (ourCrc, ourSize) = inflate()
            val (theirCrc, theirSize) = readGzipTrailer()
            if (ourCrc != theirCrc)
                error(
                    "CRC error in gunzipped content;" +
                            " ourCrc=0x${ourCrc.toString(radix = 16)}" +
                            " theirCrc=0x${theirCrc.toString(radix = 16)}"
                )
            if (ourSize != theirSize)
                error("Length differs in gunzipped content; ourSize=$ourSize theirSize=${theirSize}")
        } catch (e: ClosedReceiveChannelException) {
            throw IllegalStateException("Truncated GZIP input", e)
        }
    }
}

private suspend fun BufferReaderScope<*>.readGzipHeader() {
    if (readUShort() != GZIP_MAGIC) error("Not in GZIP format")
    if (readUByte() != 8) error("Unsupported compression method")
    val flags = readUByte()
    skipBytes(6)
    if ((flags and FEXTRA) != 0) {
        skipBytes(readUShort())
    }
    if ((flags and FNAME) != 0) {
        while (true) {
            if (readUByte() == 0)
                break
        }
    }
    if ((flags and FCOMMENT) != 0) {
        while (true) {
            if (readUByte() == 0)
                break
        }
    }
    if ((flags and FHCRC) != 0) {
        readUShort()
    }
}

private suspend fun BufferReaderScope<ByteBuffer>.inflate(): GzipTrailer {
    val crc = CRC32()
    val inflater = Inflater(true)
    try {
        while (true) {
            val buffer = nextBuffer()
            inflater.setInput(buffer)
            produce@
            while (true) {
                val output = ByteBuffer.allocate(2048)
                val bytesProduced = inflater.inflate(output)
                when {
                    bytesProduced != 0 -> {
                        output.flip()
                        crc.update(output.asReadOnlyBuffer())
                        emit(output)
                    }
                    inflater.finished() -> {
                        putBackBuffer(buffer)
                        return GzipTrailer(crc.value, inflater.bytesWritten)
                    }
                    inflater.needsDictionary() -> {
                        throw UnsupportedOperationException("inflater needs dictionary -- not implemented")
                    }
                    inflater.needsInput() -> {
                        break@produce
                    }
                    else -> {
                        error("inflater did not produce output, has not finished but does not need input - ???")
                    }
                }
            }
        }
    } finally {
        inflater.end()
    }
}

private suspend fun BufferReaderScope<*>.readGzipTrailer(): GzipTrailer {
    val theirCrc = readUInt()
    val size = readUInt()
    return GzipTrailer(theirCrc.toLong() and 0xffffffff, size.toLong())
}

private data class GzipTrailer(val theirCrc: Long, val size: Long)

private const val GZIP_MAGIC: Int = 0x8b1f

private const val FTEXT = 1 // Extra text
private const val FHCRC = 2 // Header CRC
private const val FEXTRA = 4 // Extra field
private const val FNAME = 8 // File name
private const val FCOMMENT = 16 // File comment

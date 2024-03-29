package org.araqnid.libraryversions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.araqnid.kotlin.assertthat.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.zip.GZIPOutputStream

class FlowGunzipTest {
    @Test
    fun `gunzips single byte buffer in flow`() {
        val gzippedContent = gzip("This is some test content.")

        val flow = flowOf(ByteBuffer.wrap(gzippedContent))

        val gunzippedBytes = runBlocking {
            flow.gunzip().toByteArray()
        }

        assertThat(String(gunzippedBytes), equalTo("This is some test content."))
    }

    @Test
    fun `gunzips multiple byte buffers in flow`() {
        val gzippedContent = gzip("This is some test content.")

        val flow = flowOf(
            ByteBuffer.wrap(gzippedContent, 0, 16),
            ByteBuffer.wrap(gzippedContent, 16, gzippedContent.size - 16)
        )

        val gunzippedBytes = runBlocking {
            flow.gunzip().toByteArray()
        }

        assertThat(String(gunzippedBytes), equalTo("This is some test content."))
    }

    @Test
    fun `detect input truncated in header`() {
        val gzippedContent = gzip("This is some test content.").sliceArray(0..4)

        val flow = flowOf(ByteBuffer.wrap(gzippedContent))

        val exception = try {
            runBlocking {
                flow.gunzip().toByteArray()
            }
            null
        } catch (e: Exception) {
            e
        }

        assertThat(exception, present(has(Throwable::message, present(equalTo("Truncated GZIP input")))))
    }

    @Test
    fun `detect input truncated in body`() {
        val gzippedContent = gzip("This is some test content.").sliceArray(0..16)

        val flow = flowOf(ByteBuffer.wrap(gzippedContent))

        val exception = try {
            runBlocking {
                flow.gunzip().toByteArray()
            }
            null
        } catch (e: Exception) {
            e
        }

        assertThat(exception, present(has(Throwable::message, present(equalTo("Truncated GZIP input")))))
    }

    @Test
    fun `detect input CRC mismatch`() {
        val gzippedContent = gzip("This is some test content.")
        val flow = flowOf(ByteBuffer.allocate(gzippedContent.size).apply {
            put(gzippedContent, 0, gzippedContent.size - 8)
            putInt(0)
            put(gzippedContent, gzippedContent.size - 4, 4)
            rewind()
        })

        val exception = try {
            runBlocking {
                flow.gunzip().toByteArray()
            }
            null
        } catch (e: Exception) {
            e
        }

        assertThat(exception, present(has(Throwable::message, present(containsSubstring("CRC error")))))
    }

    @Test
    fun `detect input size mismatch`() {
        val gzippedContent = gzip("This is some test content.")
        val flow = flowOf(ByteBuffer.allocate(gzippedContent.size).apply {
            put(gzippedContent, 0, gzippedContent.size - 8)
            put(gzippedContent, gzippedContent.size - 8, 4)
            putInt(0)
            rewind()
        })

        val exception = try {
            runBlocking {
                flow.gunzip().toByteArray()
            }
            null
        } catch (e: Exception) {
            e
        }

        assertThat(exception, present(has(Throwable::message, present(containsSubstring("Length differs in gunzipped content")))))
    }

    private fun gzip(text: String): ByteArray {
        val baos = ByteArrayOutputStream()
        GZIPOutputStream(baos).writer().use { writer ->
            writer.write(text)
        }
        return baos.toByteArray()
    }

    private suspend fun Flow<ByteBuffer>.toByteArray(): ByteArray {
        val outputBuffers = mutableListOf<ByteBuffer>()
        collect { outputBuffer ->
            outputBuffers += outputBuffer
        }
        val totalSize = outputBuffers.sumOf { it.limit() }
        val bytes = ByteArray(totalSize)
        var offset = 0
        for (buf in outputBuffers) {
            buf.array().copyInto(bytes, offset, 0, buf.limit())
            offset += buf.limit()
        }
        return bytes
    }
}

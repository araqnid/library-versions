package org.araqnid.libraryversions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.araqnid.libraryversions.js.assertions.assertThat
import org.araqnid.libraryversions.js.assertions.equalTo
import org.araqnid.libraryversions.js.assertions.has
import org.araqnid.libraryversions.js.assertions.present
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.zip.GZIPOutputStream

class GunzipTest {
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
    fun `detect truncated input`() {
        val gzippedContent = gzip("This is some test content.").sliceArray(0..4)

        val flow = flowOf(ByteBuffer.wrap(gzippedContent))

        val exception = try {
            runBlocking {
                flow.gunzip().toByteArray()
            }
            null
        } catch (e : Exception) {
            e
        }

        assertThat(exception, present(has(Throwable::message, present(equalTo("Truncated GZIP input")))))
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
        val totalSize = outputBuffers.sumBy { it.limit() }
        val bytes = ByteArray(totalSize)
        var offset = 0
        for (buf in outputBuffers) {
            buf.array().copyInto(bytes, offset, 0, buf.limit())
            offset += buf.limit()
        }
        return bytes
    }
}

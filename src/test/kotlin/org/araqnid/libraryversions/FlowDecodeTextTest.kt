package org.araqnid.libraryversions

import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.araqnid.libraryversions.assertions.assertThat
import org.araqnid.libraryversions.assertions.contains
import org.araqnid.libraryversions.assertions.equalTo
import org.araqnid.libraryversions.assertions.has
import org.araqnid.libraryversions.assertions.present
import org.junit.Test
import java.nio.ByteBuffer

class FlowDecodeTextTest {
    @Test
    fun decodes_binary_data_as_text() {
        val inputText = "Все счастливые семьи похожи друг на друга, каждая несчастливая семья несчастлива по-своему."
        val byteBuffers = inputText.toByteArray().splitIntoChunks().map { ByteBuffer.wrap(it)!! }.asFlow()
        val outputText = runBlocking {
            val stringBuilder = StringBuilder()
            byteBuffers.decodeText().collect { chars: CharSequence ->
                stringBuilder.append(chars)
            }
            stringBuilder.toString()
        }
        assertThat(outputText, equalTo(inputText))
    }

    @Test
    fun detects_trailing_truncated_character() {
        val inputText = "Все счастливые семьи похожи друг на друга, каждая несчастливая семья несчастлива по-своему."
        val byteBuffers = inputText.toByteArray().splitIntoChunks().let { input ->
            sequence<ByteArray> {
                yieldAll(input)
                yield(byteArrayOf(0xc2.toByte())) // 0xc2 0xa3 would be a "£" character in UTF-8 (U+00A3)
            }
        }.map { ByteBuffer.wrap(it)!! }.asFlow()
        val exception = try {
            runBlocking {
                byteBuffers.decodeText().collect()
            }
            null
        } catch (e: IllegalStateException) {
            e
        }
        assertThat(exception, present(has(Throwable::message, present(contains(Regex("invalid text: MALFORMED"))))))
    }

    private fun ByteArray.splitIntoChunks(chunkLength: Int = 10) = Sequence {
        object : Iterator<ByteArray> {
            private var pos = 0

            override fun hasNext(): Boolean = pos < size

            override fun next(): ByteArray {
                val chunk = if ((pos + chunkLength) > size) sliceArray(pos until size) else sliceArray(pos until pos + chunkLength)
                pos += chunkLength
                return chunk
            }
        }
    }
}

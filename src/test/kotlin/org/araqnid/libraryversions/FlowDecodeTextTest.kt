package org.araqnid.libraryversions

import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.araqnid.libraryversions.assertions.assertThat
import org.araqnid.libraryversions.assertions.contains
import org.araqnid.libraryversions.assertions.equalTo
import org.araqnid.libraryversions.assertions.has
import org.araqnid.libraryversions.assertions.present
import org.junit.Assert.assertThrows
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
        val byteBuffers = (inputText.toByteArray().splitIntoChunks() + listOf(byteArrayOf(0xc2.toByte())))
            .map { ByteBuffer.wrap(it)!! }.asFlow()
        val exception = assertThrows(IllegalStateException::class.java) {
            runBlocking {
                byteBuffers.decodeText().collect()
            }
        }
        assertThat(exception, present(has(Throwable::message, present(contains(Regex("invalid text: MALFORMED"))))))
    }

    private fun ByteArray.splitIntoChunks(chunkLength: Int = 10): Sequence<ByteArray> {
        return (0 until size step chunkLength).asSequence().map { pos ->
            if ((pos + chunkLength) > size)
                sliceArray(pos until size)
            else
                sliceArray(pos until pos + chunkLength)
        }
    }
}

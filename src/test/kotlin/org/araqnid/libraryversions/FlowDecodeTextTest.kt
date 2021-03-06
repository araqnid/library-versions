package org.araqnid.libraryversions

import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.araqnid.kotlin.assertthat.assertThat
import org.araqnid.kotlin.assertthat.contains
import org.araqnid.kotlin.assertthat.equalTo
import org.araqnid.kotlin.assertthat.has
import org.araqnid.kotlin.assertthat.present
import org.junit.Assert.assertThrows
import org.junit.Test
import java.nio.ByteBuffer

class FlowDecodeTextTest {
    @Test
    fun decodes_binary_data_as_text() {
        val inputText = "Все счастливые семьи похожи друг на друга, каждая несчастливая семья несчастлива по-своему."
        val byteBuffers = inputText.toByteArray().chunkedSequence(10).map { ByteBuffer.wrap(it)!! }.asFlow()
        val outputText = runBlocking {
            byteBuffers.decodeText().joinToString("")
        }
        assertThat(outputText, equalTo(inputText))
        inputText.chunkedSequence(10)
    }

    @Test
    fun detects_trailing_truncated_character() {
        val inputText = "Все счастливые семьи похожи друг на друга, каждая несчастливая семья несчастлива по-своему."
        val byteBuffers = (inputText.toByteArray().chunkedSequence(10) + listOf(byteArrayOf(0xc2.toByte())))
            .map { ByteBuffer.wrap(it)!! }.asFlow()
        val exception = assertThrows(IllegalStateException::class.java) {
            runBlocking {
                byteBuffers.decodeText().collect()
            }
        }
        assertThat(exception, present(has(Throwable::message, present(contains(Regex("invalid text: MALFORMED"))))))
    }

    private fun ByteArray.chunkedSequence(size: Int): Sequence<ByteArray> {
        return (indices step size).asSequence().map { pos ->
            sliceArray(pos until (pos + size).coerceAtMost(this.size))
        }
    }
}

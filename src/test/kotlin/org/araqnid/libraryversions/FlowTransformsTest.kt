package org.araqnid.libraryversions

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.nio.ByteBuffer

class FlowTransformsTest {
    @Test
    fun decodes_binary_data_as_text() {
        val inputText = "Все счастливые семьи похожи друг на друга, каждая несчастливая семья несчастлива по-своему."
        val byteBuffers = inputText.splitIntoChunks().map { ByteBuffer.wrap(it.toByteArray())!! }.asFlow()
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
    fun marshals_character_sequences_into_lines() {
        val charSequences = flowOf("line:", " 1\nline: 2\n", "line: 3\n", "li", "ne: ", "4\n")
        val lines = runBlocking {
            charSequences.splitByLines().toList()
        }
        assertThat(lines, equalTo(listOf("line: 1", "line: 2", "line: 3", "line: 4")))
    }

    private fun String.splitIntoChunks(chunkLength: Int = 10) = object : Sequence<String> {
        override fun iterator() = object: Iterator<String> {
            private var pos = 0

            override fun hasNext(): Boolean = pos < length

            override fun next(): String {
                val chunk = if ((pos + chunkLength) > length) substring(pos) else substring(pos, pos + chunkLength)
                pos += chunkLength
                return chunk
            }
        }
    }
}

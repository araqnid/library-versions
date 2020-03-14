package org.araqnid.libraryversions

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.araqnid.libraryversions.assertions.assertThat
import org.araqnid.libraryversions.assertions.equalTo
import org.junit.Test

class FlowSplitByLinesTest {
    @Test
    fun marshals_character_sequences_into_lines() {
        val charSequences = flowOf("line:", " 1\nline: 2\n", "line: 3\n", "li", "ne: ", "4\n")
        val lines = runBlocking {
            charSequences.splitByLines().toList()
        }
        assertThat(lines, equalTo(listOf("line: 1", "line: 2", "line: 3", "line: 4")))
    }

    @Test
    fun includes_trailing_chunk() {
        val charSequences = flowOf("line:", " 1\ntrailer")
        val lines = runBlocking {
            charSequences.splitByLines().toList()
        }
        assertThat(lines, equalTo(listOf("line: 1", "trailer")))
    }

    @Test
    fun marshals_character_sequences_into_lines_with_supplied_separator() {
        val charSequences = flowOf("line:", " 1\r\nline: 2\r\n", "line: 3\r\n", "li", "ne: ", "4\r\n")
        val lines = runBlocking {
            charSequences.splitByLines("\r\n").toList()
        }
        assertThat(lines, equalTo(listOf("line: 1", "line: 2", "line: 3", "line: 4")))
    }
}

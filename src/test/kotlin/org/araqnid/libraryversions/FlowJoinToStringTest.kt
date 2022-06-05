package org.araqnid.libraryversions

import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.araqnid.kotlin.assertthat.assertThat
import org.araqnid.kotlin.assertthat.equalTo
import java.nio.CharBuffer
import kotlin.test.Test

class FlowJoinToStringTest {
    @Test
    fun `joins strings together with separator`() {
        assertThat(
            runBlocking { flowOf("foo", "bar").joinToString() },
            equalTo(listOf("foo", "bar").joinToString())
        )
    }

    @Test
    fun `joins characters together with separator`() {
        assertThat(
            runBlocking { flowOf('a', 'b').joinToString() },
            equalTo(listOf('a', 'b').joinToString())
        )
    }

    @Test
    fun `joins char buffers together with separator`() {
        assertThat(
            runBlocking { flowOf(CharBuffer.wrap("foo"), CharBuffer.wrap("bar")).joinToString() },
            equalTo(listOf(CharBuffer.wrap("foo"), CharBuffer.wrap("bar")).joinToString())
        )
    }

    data class Other(val id: Int)

    @Test
    fun `joins other objects together using their toString`() {
        assertThat(
            runBlocking { flowOf(Other(id = 1), Other(id = 2)).joinToString() },
            equalTo(listOf(Other(id = 1), Other(id = 2)).joinToString())
        )
    }

    @Test
    fun `handles empty flow`() {
        assertThat(
            runBlocking { emptyFlow<String>().joinToString() },
            equalTo(emptyList<String>().joinToString())
        )
    }

    @Test
    fun `prepends prefix and appends postfix`() {
        assertThat(
            runBlocking { flowOf("foo", "bar").joinToString(separator = ", ", prefix = "{", postfix = "}") },
            equalTo(listOf("foo", "bar").joinToString(separator = ", ", prefix = "{", postfix = "}"))
        )
    }

    @Test
    fun `prepends prefix and appends postfix even to empty flow`() {
        assertThat(
            runBlocking { emptyFlow<String>().joinToString(separator = ", ", prefix = "{", postfix = "}") },
            equalTo(listOf<String>().joinToString(separator = ", ", prefix = "{", postfix = "}"))
        )
    }

    @Test
    fun `applies transform to each element`() {
        assertThat(
            runBlocking { flowOf("foo", "bar").joinToString { it.uppercase() } },
            equalTo(listOf("foo", "bar").joinToString { it.uppercase() })
        )
    }
}
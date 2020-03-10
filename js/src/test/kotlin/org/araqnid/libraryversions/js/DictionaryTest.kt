package org.araqnid.libraryversions.js

import kotlinext.js.js
import org.araqnid.libraryversions.js.axios.Dictionary
import org.araqnid.libraryversions.js.axios.DictionaryEntry
import org.araqnid.libraryversions.js.axios.asMap
import org.araqnid.libraryversions.js.axios.component1
import org.araqnid.libraryversions.js.axios.component2
import org.araqnid.libraryversions.js.axios.dictionaryOf
import org.araqnid.libraryversions.js.axios.entries
import org.araqnid.libraryversions.js.axios.get
import org.araqnid.libraryversions.js.axios.key
import org.araqnid.libraryversions.js.axios.keys
import org.araqnid.libraryversions.js.axios.value
import org.araqnid.libraryversions.js.axios.values
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DictionaryTest {
    @Test
    fun dictionary_object_access() {
        val dic = js {
            this.foo = 1
            this.bar = 2
        }.unsafeCast<Dictionary<Int>>()
        assertEquals(1, dic["foo"])
        assertEquals(2, dic["bar"])
        assertNull(dic["quux"])
    }

    @Test
    fun dictionary_keys_property() {
        val dic = js {
            this.foo = 1
            this.bar = 2
        }.unsafeCast<Dictionary<Int>>()
        val keys: Array<String> = dic.keys
        assertEquals(setOf("foo", "bar"), keys.toSet())
    }

    @Test
    fun dictionary_values_property() {
        val dic = js {
            this.foo = 1
            this.bar = 2
        }.unsafeCast<Dictionary<Int>>()
        val values: Array<Int> = dic.values
        assertEquals(setOf(1, 2), values.toSet())
    }

    @Test
    fun dictionary_entries_property() {
        val dic = js {
            this.foo = 1
            this.bar = 2
        }.unsafeCast<Dictionary<Int>>()
        val entries: Array<DictionaryEntry<Int>> = dic.entries
        assertEquals("foo", entries[0].key)
        assertEquals(1, entries[0].value)
        val (key1, value1) = entries[0]
        assertEquals("foo", key1)
        assertEquals(1, value1)
        assertEquals("bar", entries[1].key)
        assertEquals(2, entries[1].value)
    }

    @Test
    fun view_as_map() {
        val dic = js {
            this.foo = 1
            this.bar = 2
        }.unsafeCast<Dictionary<Int>>()
        val map = dic.asMap()
        assertEquals(mapOf("foo" to 1, "bar" to 2), map)
        assertEquals(setOf("foo", "bar"), map.keys)
        assertEquals(setOf(1, 2), map.values.toSet())
        assertEquals(setOf("foo" to 1, "bar" to 2), map.entries.map { (key, value) -> key to value }.toSet())
        assertEquals(1, map["foo"])
        assertEquals(2, map["bar"])
        assertNull(map["quux"])
    }

    @Test
    fun dictionary_creator_function() {
        val dic = dictionaryOf("foo" to 1, "bar" to 2)
        assertEquals("""{"foo":1,"bar":2}""", JSON.stringify(dic))
    }
}

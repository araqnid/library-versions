package org.araqnid.libraryversions

import kotlinext.js.js
import org.araqnid.libraryversions.assertions.assertThat
import org.araqnid.libraryversions.assertions.equalTo
import org.araqnid.libraryversions.assertions.isAbsent
import org.araqnid.libraryversions.assertions.isEqualTo
import kotlin.test.Test

class DictionaryTest {
    @Test
    fun dictionary_object_access() {
        val dic = js {
            this.foo = 1
            this.bar = 2
        }.unsafeCast<Dictionary<Int>>()
        assertThat(dic["foo"], isEqualTo(1))
        assertThat(dic["bar"], isEqualTo(2))
        assertThat(dic["quux"], isAbsent)
    }

    @Test
    fun dictionary_keys_property() {
        val dic = js {
            this.foo = 1
            this.bar = 2
        }.unsafeCast<Dictionary<Int>>()
        val keys: Array<String> = dic.keys
        assertThat(keys.toSet(), equalTo(setOf("foo", "bar")))
    }

    @Test
    fun dictionary_values_property() {
        val dic = js {
            this.foo = 1
            this.bar = 2
        }.unsafeCast<Dictionary<Int>>()
        val values: Array<Int> = dic.values
        assertThat(values.toSet(), equalTo(setOf(1, 2)))
    }

    @Test
    fun dictionary_entries_property() {
        val dic = js {
            this.foo = 1
            this.bar = 2
        }.unsafeCast<Dictionary<Int>>()

        val entries: Array<DictionaryEntry<Int>> = dic.entries
        assertThat(entries[0].key, equalTo("foo"))
        assertThat(entries[0].value, equalTo(1))
        assertThat(entries[1].key, equalTo("bar"))
        assertThat(entries[1].value, equalTo(2))

        val (key1, value1) = entries[0]
        assertThat(key1, equalTo("foo"))
        assertThat(value1, equalTo(1))
    }

    @Test
    fun view_as_map() {
        val dic = js {
            this.foo = 1
            this.bar = 2
        }.unsafeCast<Dictionary<Int>>()

        val map = dic.asMap()
        assertThat(map, equalTo(mapOf("foo" to 1, "bar" to 2)))
        assertThat(map.keys, equalTo(setOf("foo", "bar")))
        assertThat(map.values.toSet(), equalTo(setOf(1, 2)))
        assertThat(map.entries.map { (key, value) -> key to value }.toSet(), equalTo(setOf("foo" to 1, "bar" to 2)))
        assertThat(map["foo"], isEqualTo(1))
        assertThat(map["bar"], isEqualTo(2))
        assertThat(map["quux"], isAbsent)
    }

    @Test
    fun dictionary_creator_function() {
        val dic = dictionaryOf("foo" to 1, "bar" to 2)
        assertThat(JSON.stringify(dic), equalTo("""{"foo":1,"bar":2}"""))
    }
}

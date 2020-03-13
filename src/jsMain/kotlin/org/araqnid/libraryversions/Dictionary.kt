package org.araqnid.libraryversions

import kotlinext.js.Object

/**
 * Javascript object with native (String) properties and keys of type V
 * @param V type of property values
 */
external interface Dictionary<out V : Any>

/**
 * Gets a single property from the JS dictionary object.
 */
inline operator fun <V : Any> Dictionary<V>.get(name: String) = this.asDynamic()[name].unsafeCast<V?>()

/**
 * Views a JS dictionary as a Kotlin map.
 *
 * Changes to the dictionary will be reflected in the map view.
 */
fun <V : Any> Dictionary<V>.asMap(): Map<String, V> = JsDictionaryMap(
        this)

/**
 * Directly read the keys from a dictionary object.
 *
 * This is an interface to `Object.keys` on the dictionary.
 */
inline val Dictionary<*>.keys: Array<String>
    get() = Object.keys(this)

/**
 * Directly read the values from a dictionary object.
 *
 * This is an interface to `Object.values` on the dictionary.
 */
inline val <V : Any> Dictionary<V>.values: Array<V>
    get() {
        @Suppress("UNUSED_VARIABLE") val underlying = this
        return js("Object.values(underlying)").unsafeCast<Array<V>>()
    }

/**
 * Actually an array of 2 elements, as returned by `Object.entries`
 */
external interface DictionaryEntry<V : Any>

inline val DictionaryEntry<*>.key: String
    get() = this.unsafeCast<Array<String>>()[0]
inline val <V : Any> DictionaryEntry<V>.value: V
    get() = this.unsafeCast<Array<V>>()[1]
inline operator fun DictionaryEntry<*>.component1(): String = key
inline operator fun <V : Any> DictionaryEntry<V>.component2(): V = value

/**
 * Directly read the entries (key-value pairs) from a dictionary object.
 *
 * This is an interface to `Object.entries` on the dictionary.
 */
inline val <V : Any> Dictionary<V>.entries: Array<DictionaryEntry<V>>
    get() {
        @Suppress("UNUSED_VARIABLE") val underlying = this
        return js("Object.entries(underlying)").unsafeCast<Array<DictionaryEntry<V>>>()
    }

/**
 * Convert a Kotlin Map into a JS dictionary.
 * @param V Types of map values, to be properties of the dictionary object
 */
@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
fun <V : Any> Map<String, V>.toDictionary(): Dictionary<V> {
    val dict = js("{}")
    forEach { (key, value) ->
        dict[key] = value
    }
    return dict as Dictionary<V>
}

/**
 * Returns a new dictionary with the specified contents.
 *
 * The iteration order of the pairs may be preserved by the platform.
 */
fun <V : Any> dictionaryOf(vararg pairs: Pair<String, V>): Dictionary<V> = mapOf(*pairs).toDictionary()

private class JsDictionaryMap<out V : Any>(@JsName("underlyingDictionary") private val dictionary: Dictionary<V>) : AbstractMap<String, V>() {
    override val keys
        get() = object : AbstractSet<String>() {
            override val size: Int
                get() = keysArray.size

            override fun iterator() = keysArray.iterator()
        }

    override val values: Collection<V>
        get() = object : AbstractCollection<V>() {
            override val size: Int
                get() = valuesArray.size

            override fun iterator() = valuesArray.iterator()
        }

    override val entries
        get() = object : AbstractSet<Map.Entry<String, V>>() {
            override val size: Int
                get() = keysArray.size

            override fun iterator() = object : Iterator<Map.Entry<String, V>> {
                private val underlying = keysArray.iterator()

                override fun hasNext(): Boolean {
                    return underlying.hasNext()
                }

                override fun next(): Map.Entry<String, V> {
                    val key = underlying.next()
                    return MapEntry(key, dictionary[key]!!)
                }
            }
        }

    override fun get(key: String) = dictionary[key]

    override val size: Int
        get() = keysArray.size

    private val keysArray: Array<String>
        get() = Object.keys(dictionary)
    private val valuesArray: Array<V>
        get() = js("Object.values(this.underlyingDictionary)").unsafeCast<Array<V>>()
}

private data class MapEntry<V : Any>(override val key: String, override val value: V) : Map.Entry<String,  V> {
    override fun toString(): String = "$key=$value"
}

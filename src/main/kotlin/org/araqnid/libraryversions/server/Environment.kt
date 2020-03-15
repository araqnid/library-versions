package org.araqnid.libraryversions.server

import kotlin.reflect.KProperty

val env = EnvironmentDelegate { it }
fun <T> env(finisher: (String) -> T) =
    EnvironmentDelegate(finisher)

class EnvironmentDelegate<T>(private val finisher: (String) -> T) {
    operator fun getValue(source: Nothing?, property: KProperty<*>): T {
        val key = property.name.toShoutyName()
        val value = System.getenv(key) ?: ""
        check(value != "") { "$key must be set" }
        return finisher(value)
    }
}

private fun CharSequence.toShoutyName(): String {
    return buildString {
        var insertUnderscore = false
        for (c in this@toShoutyName) {
            if (c.isUpperCase() && insertUnderscore) {
                append('_')
                insertUnderscore = false
            }
            append(c.toUpperCase())
            if (!c.isUpperCase()) {
                insertUnderscore = true
            }
        }
    }
}

package org.araqnid.libraryversions

import kotlinx.coroutines.flow.Flow

suspend fun <T> Flow<T>.joinToString(
    separator: CharSequence = ", ",
    prefix: CharSequence = "",
    postfix: CharSequence = "",
    transform: ((T) -> CharSequence)? = null
) = buildString {
    append(prefix)
    var isFirst = true
    collect { element ->
        if (isFirst)
            isFirst = false
        else
            append(separator)
        when {
            transform != null -> append(transform(element))
            element is CharSequence -> append(element)
            element is Char -> append(element)
            else -> append(element.toString())
        }
    }
    append(postfix)
}

package org.araqnid.libraryversions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.single

suspend fun <T> Flow<T>.joinToString(
    separator: CharSequence = ",",
    prefix: CharSequence = "",
    postfix: CharSequence = "",
    transform: ((T) -> CharSequence)? = null
): String {
    val stringBuilder = StringBuilder()
    flow {
        stringBuilder.append(prefix)
        var count = 0
        collect { element ->
            if (count++ == 0)
                stringBuilder.append(prefix)
            else
                stringBuilder.append(separator)
            when {
                transform != null -> stringBuilder.append(transform(element))
                element is CharSequence -> stringBuilder.append(element)
                element is Char -> stringBuilder.append(element)
                else -> stringBuilder.append(element.toString())
            }
        }
        stringBuilder.append(postfix)
        emit(stringBuilder.toString())
    }.single()
    return stringBuilder.toString()
}


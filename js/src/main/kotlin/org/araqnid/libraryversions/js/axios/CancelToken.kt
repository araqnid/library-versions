package org.araqnid.libraryversions.js.axios

import kotlin.js.Promise

external interface CancelTokenSource {
    val token: CancelToken
    fun cancel(message: String = definedExternally)
}

external interface CancelToken {
    val promise: Promise<Cancel>
    val reason: Cancel?
    fun throwIfRequested()
}

external interface Cancel {
    val message: String
}

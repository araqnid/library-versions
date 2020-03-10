package org.araqnid.libraryversions.js.axios

external interface Response<T> {
    val data: T
    val status: Int
    val statusText: String
    val headers: Dictionary<String>
    val config: RequestConfig
    val request: dynamic
}

package org.araqnid.libraryversions.js.axios

external interface AxiosResponse<T> {
    val data: T
    val status: Int
    val statusText: String
    val headers: Dictionary<String>
    val config: AxiosRequestConfig
    val request: dynamic
}

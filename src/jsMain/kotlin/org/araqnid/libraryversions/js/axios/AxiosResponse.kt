package org.araqnid.libraryversions.js.axios

import org.araqnid.libraryversions.Dictionary

external interface AxiosResponse<T> {
    val data: T
    val status: Int
    val statusText: String
    val headers: Dictionary<String>
    val config: AxiosRequestConfig
    val request: dynamic
}

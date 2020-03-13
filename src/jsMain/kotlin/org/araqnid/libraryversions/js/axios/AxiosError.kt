package org.araqnid.libraryversions.js.axios

external interface AxiosError<T>{
    val config: AxiosRequestConfig
    val code: String?
    val request: Any?
    val response: AxiosResponse<T>?
    val isAxiosError: Boolean
    fun toJSON(): Any
}

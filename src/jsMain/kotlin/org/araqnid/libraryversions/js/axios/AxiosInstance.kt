package org.araqnid.libraryversions.js.axios

import kotlin.js.Promise

open external class AxiosInstance {
    val defaults: AxiosRequestConfig
    fun <T> request(config: AxiosRequestConfig = definedExternally): Promise<AxiosResponse<T>>
    fun <T> get(url: String, config: AxiosRequestConfig = definedExternally): Promise<AxiosResponse<T>>
}

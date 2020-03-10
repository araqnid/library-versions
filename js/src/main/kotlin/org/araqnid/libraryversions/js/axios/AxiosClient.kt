package org.araqnid.libraryversions.js.axios

import kotlin.js.Promise

external open class AxiosClient {
    fun <T> request(config: RequestConfig = definedExternally): Promise<Response<T>>
    fun <T> get(url: String, config: RequestConfig = definedExternally): Promise<Response<T>>
}

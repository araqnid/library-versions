package org.araqnid.libraryversions.js.axios

@JsModule("axios")
external object Axios : AxiosClient {
    fun isCancel(thrown: Throwable): Boolean

    object CancelToken { // CancelTokenStatic
        fun source(): CancelTokenSource
    }

    fun create(config: RequestConfig = definedExternally): AxiosClient
}

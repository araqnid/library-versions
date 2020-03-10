package org.araqnid.libraryversions.js.axios

@JsModule("axios")
external object Axios : AxiosInstance {
    fun isCancel(thrown: Throwable): Boolean

    object CancelToken {
        fun source(): CancelTokenSource
    }

    fun create(config: AxiosRequestConfig = definedExternally): AxiosInstance
}

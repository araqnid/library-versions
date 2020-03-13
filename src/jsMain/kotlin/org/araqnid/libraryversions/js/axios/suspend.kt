package org.araqnid.libraryversions.js.axios

import kotlinext.js.assign
import kotlinext.js.jsObject
import kotlinx.coroutines.suspendCancellableCoroutine
import org.araqnid.libraryversions.Dictionary

suspend fun <T : Any> AxiosInstance.getJson(url: String, config: AxiosRequestConfig = noRequestConfig): AxiosResponse<T> =
        doRequest(assign(config) {
            this.url = url
            this.method = "get"
            this.responseType = "json"
            this.headers = config.headers.extend("Accept", "application/json")
        })

suspend fun AxiosInstance.getText(url: String, config: AxiosRequestConfig = noRequestConfig): AxiosResponse<String> =
        doRequest(assign(config) {
            this.url = url
            this.method = "get"
            this.responseType = "text"
            this.headers = config.headers.extend("Accept", "text/plain, text/*;q=0.8, application/xml;q=0.7")
        })

private inline fun <V : Any> Dictionary<V>.extend(key: String, value: V): Dictionary<V> {
    return assign(this) {
        this.asDynamic()[key] = value
    }
}

private val noRequestConfig = jsObject<AxiosRequestConfig> { }

private suspend fun <T> AxiosInstance.doRequest(mutableConfig: AxiosRequestConfig = noRequestConfig): AxiosResponse<T> {
    return suspendCancellableCoroutine { cont ->
        val cancelTokenSource = Axios.CancelToken.source()
        mutableConfig.cancelToken = cancelTokenSource.token
        request<T>(mutableConfig).then(
                { value -> cont.resumeWith(Result.success(value)) },
                { err -> cont.resumeWith(Result.failure(err)) }
        )
        cont.invokeOnCancellation { ex ->
            cancelTokenSource.cancel(ex?.message ?: "Cancelled")
        }
    }
}

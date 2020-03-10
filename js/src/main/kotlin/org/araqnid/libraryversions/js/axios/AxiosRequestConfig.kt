package org.araqnid.libraryversions.js.axios

external interface AxiosRequestConfig {
    var withCredentials: Boolean // default: false
    var url: String? // default: none
    var method: String // default: GET
    var baseURL: String? // default: none
    var headers: Dictionary<String> // e.g. (not default): { "X-Requested-With": "XMLHttpRequest" }
    @JsName("timeout")
    var timeoutMillis: Int // default: ?
    var responseType: String // default: json
    var responseEncoding: String // default: utf8
    var xsrfCookieName: String // default "XSRF-TOKEN"
    var xsrfHeaderName: String // default "X-XSRF-TOKEN"
    var onUploadProgress: (ProgressEvent) -> Unit // default no-op
    var onDownloadProgress: (ProgressEvent) -> Unit // default no-op
    var maxContentLength: Int // default: ?
    var maxRedirects: Int // default: 5
    var socketPath: String? // default: null
    var proxy: AxiosProxyConfig?
    var cancelToken: CancelToken? // default: none
}

external interface AxiosProxyConfig {
    var host: String
    var port: Int
    var auth: AuthConfig?

    interface AuthConfig {
        var username: String
        var password: String
    }
}

external interface ProgressEvent

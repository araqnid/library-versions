package org.araqnid.libraryversions

import org.araqnid.libraryversions.js.axios.Axios
import org.araqnid.libraryversions.js.axios.AxiosInstance
import org.araqnid.libraryversions.js.axios.getText

class AxiosHttpFetcher(val axios: AxiosInstance = Axios) : HttpFetcher {
    override suspend fun getText(url: String): HttpFetcher.Response<String> {
        val response = axios.getText(url)
        return HttpFetcher.Response(statusCode = response.status, data = response.data)
    }

    override suspend fun getBinary(url: String): HttpFetcher.Response<ByteArray> {
        throw UnsupportedOperationException()
    }
}

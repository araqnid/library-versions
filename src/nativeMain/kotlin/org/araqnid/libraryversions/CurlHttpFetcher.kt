package org.araqnid.libraryversions

import curl.CURLE_OK
import curl.CURLOPT_HEADERDATA
import curl.CURLOPT_HEADERFUNCTION
import curl.CURLOPT_URL
import curl.CURLOPT_WRITEDATA
import curl.CURLOPT_WRITEFUNCTION
import curl.curl_easy_cleanup
import curl.curl_easy_init
import curl.curl_easy_perform
import curl.curl_easy_setopt
import curl.curl_easy_strerror
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toKString
import platform.posix.size_t

object CurlHttpFetcher : HttpFetcher {
    override suspend fun getText(url: String): HttpFetcher.Response<String> {
        val request = CurlHttpRequest()
        return try {
            request.getText(url)
        } finally {
            request.dispose()
        }
    }

    override suspend fun getBinary(url: String): HttpFetcher.Response<ByteArray> {
        throw UnsupportedOperationException()
    }
}

private class CurlHttpRequest {
    private val curl = curl_easy_init() ?: error("Failed to initialise curl")
    private val stableRef = StableRef.create(this)
    private var headerCount = 0
    private var statusLine: String? = null
    private val bodyParts = mutableListOf<String>()

    fun getText(url: String): HttpFetcher.Response<String> {
        curl_easy_setopt(curl, CURLOPT_URL, url)
        val header = staticCFunction(::header_callback)
        curl_easy_setopt(curl, CURLOPT_HEADERFUNCTION, header)
        curl_easy_setopt(curl, CURLOPT_HEADERDATA, stableRef.asCPointer())
        val writeData = staticCFunction(::write_callback)
        curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, writeData)
        curl_easy_setopt(curl, CURLOPT_WRITEDATA, stableRef.asCPointer())
        val res = curl_easy_perform(curl)
        if (res != CURLE_OK)
            error("curl_easy_perform() failed: ${curl_easy_strerror(res)?.toKString()}")
        return HttpFetcher.Response(200, bodyParts.joinToString(separator = ""))
    }

    fun header(content: String) {
        if (headerCount == 0)
            statusLine = content
        ++headerCount
    }

    fun body(content: String) {
        bodyParts += content
    }

    fun dispose() {
        curl_easy_cleanup(curl)
        stableRef.dispose()
    }
}

private fun CPointer<ByteVar>.toKString(length: Int): String {
    val bytes = this.readBytes(length)
    return bytes.decodeToString()
}

private fun header_callback(buffer: CPointer<ByteVar>?, size: size_t, nitems: size_t, userdata: COpaquePointer?): size_t {
    if (buffer == null) return 0u
    if (userdata != null) {
        val data = buffer.toKString((size * nitems).toInt()).trim()
        val httpRequest = userdata.asStableRef<CurlHttpRequest>().get()
        httpRequest.header(data)
    }
    return size * nitems
}

private fun write_callback(buffer: CPointer<ByteVar>?, size: size_t, nitems: size_t, userdata: COpaquePointer?): size_t {
    if (buffer == null) return 0u
    if (userdata != null) {
        val data = buffer.toKString((size * nitems).toInt()).trim()
        val httpRequest = userdata.asStableRef<CurlHttpRequest>().get()
        httpRequest.body(data)
    }
    return size * nitems
}

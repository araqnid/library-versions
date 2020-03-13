package org.araqnid.libraryversions

import kotlinx.coroutines.future.await
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class JdkHttpFetcher(val httpClient: HttpClient = HttpClient.newHttpClient()) : HttpFetcher {
    override suspend fun getText(url: String): HttpFetcher.Response<String> =
            request(url, HttpResponse.BodyHandlers.ofString())

    override suspend fun getBinary(url: String): HttpFetcher.Response<ByteArray> =
            request(url, HttpResponse.BodyHandlers.ofByteArray())

    private suspend fun <T> request(url: String, bodyHandler: HttpResponse.BodyHandler<T>): HttpFetcher.Response<T> {
        val request = HttpRequest.newBuilder().uri(URI(url)).build()
        val response = httpClient.sendAsync(request, bodyHandler).await()
        return HttpFetcher.Response(statusCode = response.statusCode(), data = response.body())
    }
}

package org.araqnid.libraryversions

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.future.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.ByteBuffer

object NodeJsResolver : Resolver {
    private const val url = "https://nodejs.org/dist/index.json"

    @OptIn(FlowPreview::class)
    override fun findVersions(httpClient: HttpClient): Flow<String> {
        return flow {
            val request = HttpRequest.newBuilder(URI(url)).header("Accept-Encoding", "gzip").build()
            val response = httpClient.sendAsync(request, flowBodyHandler)
                .await()
                .also { verifyOk(request, it) }

            val buffer = response.body().flatMapConcat { it.asFlow() }.gunzipTE(response).toList().aggregate()
            val format = Json {
                ignoreUnknownKeys = true
            }
            val (ltsVersions, nonLtsVersions) = format.decodeFromString<List<Release>>(
                buffer.array().toString(Charsets.UTF_8)
            )
                .partition { it.isLTS }
            ltsVersions.maxByOrNull { it.parsedVersion }?.let { emit("${it.version} ${it.lts.jsonPrimitive.content}") }
            nonLtsVersions.maxByOrNull { it.parsedVersion }?.let { emit(it.version) }
        }
    }

    private fun Flow<ByteBuffer>.gunzipTE(response: HttpResponse<*>): Flow<ByteBuffer> {
        return when (val contentEncoding: String? = response.headers().firstValue("content-encoding").orElse(null)) {
            null -> this
            "gzip" -> this.gunzip()
            else -> error("Unhandled Content-Encoding: $contentEncoding")
        }
    }

    private fun List<ByteBuffer>.aggregate(): ByteBuffer {
        val output = ByteBuffer.allocate(sumOf { it.limit() })
        for (buffer in this) {
            output.put(buffer)
        }
        output.rewind()
        return output
    }

    @Serializable
    data class Release(
        val version: String,
        val lts: JsonElement,
        val security: Boolean
    ) {
        val parsedVersion by lazy { parseVersion(version) }
        val isLTS: Boolean
            get() = lts.jsonPrimitive.let { it.isString || it.content != "false" }
    }

    override fun toString(): String = "NodeJs"
}

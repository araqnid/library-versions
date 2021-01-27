package org.araqnid.libraryversions

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdNodeBasedDeserializer
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.future.await
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.ByteBuffer

object NodeJsResolver : Resolver {
    private const val url = "https://nodejs.org/dist/index.json"
    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())

    @OptIn(FlowPreview::class)
    override fun findVersions(httpClient: HttpClient): Flow<String> {
        return flow {
            val request = HttpRequest.newBuilder(URI(url)).header("Accept-Encoding", "gzip").build()
            val response = httpClient.sendAsync(request, flowBodyHandler)
                .await()
                .also { verifyOk(request, it) }

            val buffer = response.body().flatMapConcat { it.asFlow() }.gunzipTE(response).toList().aggregate()
            val (ltsVersions, nonLtsVersions) = objectMapper.readValue<List<Release>>(buffer.array())
                .partition { it.lts != null }
            ltsVersions.maxByOrNull { it.parsedVersion }?.let { emit("${it.version} ${it.lts}") }
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
        val output = ByteBuffer.allocate(sumBy { it.limit() })
        for (buffer in this) {
            output.put(buffer)
        }
        output.rewind()
        return output
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Release(
        val version: String,
        @JsonDeserialize(using = LTSVersionDeserializer::class)
        val lts: String?,
        val security: Boolean
    ) {
        val parsedVersion by lazy { parseVersion(version) }
    }

    class LTSVersionDeserializer : StdNodeBasedDeserializer<String>(String::class.java) {
        override fun convert(root: JsonNode, ctxt: DeserializationContext): String? {
            return if (root.isBoolean)
                null
            else
                root.asText()
        }
    }

    override fun toString(): String = "NodeJs"
}

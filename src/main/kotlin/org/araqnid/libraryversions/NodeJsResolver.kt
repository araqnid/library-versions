package org.araqnid.libraryversions

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdNodeBasedDeserializer
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.future.await
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

object NodeJsResolver : Resolver {
    private const val url = "https://nodejs.org/dist/index.json"
    private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())

    override fun findVersions(httpClient: HttpClient): Flow<String> {
        return flow {
            val request = HttpRequest.newBuilder(URI(url)).build()
            val response = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                    .await()
                    .also { verifyOk(request, it) }

            val (ltsVersions, nonLtsVersions) = objectMapper.readValue<List<Release>>(response.body()).partition { it.lts != null }
            ltsVersions.maxBy { it.parsedVersion }?.let { emit("${it.version} ${it.lts}") }
            nonLtsVersions.maxBy { it.parsedVersion }?.let { emit(it.version) }
        }
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

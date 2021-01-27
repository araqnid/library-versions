package org.araqnid.libraryversions

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.future.await
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.ByteBuffer

object GradleResolver : Resolver {
    private const val url = "https://gradle.org/releases/"
    private val versionPattern = Regex("""<a name="([0-9]\.[0-9.]+)">""")

    @OptIn(FlowPreview::class)
    override fun findVersions(httpClient: HttpClient) = flow {
        val request = HttpRequest.newBuilder().uri(URI(url)).header("Accept-Encoding", "gzip").build()
        val response = httpClient.sendAsync(request, flowBodyHandler)
            .await()
            .also { verifyOk(request, it) }

        emitAll(response.body()
            .flatMapConcat { it.asFlow() }
            .gunzipTE(response)
            .decodeText()
            .splitByLines()
            .extractVersions()
            .take(3)
        )
    }

    private fun Flow<ByteBuffer>.gunzipTE(response: HttpResponse<*>): Flow<ByteBuffer> {
        return when (val contentEncoding: String? = response.headers().firstValue("content-encoding").orElse(null)) {
            null -> this
            "gzip" -> this.gunzip()
            else -> error("Unhandled Content-Encoding: $contentEncoding")
        }
    }

    private fun Flow<String>.extractVersions() = flow {
        var lastPrefix: String? = null
        collect { line ->
            versionPattern.find(line)?.let { matchResult ->
                val version = parseVersion(matchResult.groupValues[1])
                val versionPrefix = "${version.parts[0].number}.${version.parts[1].number}"
                if (lastPrefix == null || lastPrefix != versionPrefix) {
                    emit(version.string)
                    lastPrefix = versionPrefix
                }
            }
        }
    }

    override fun toString() = "Gradle"
}

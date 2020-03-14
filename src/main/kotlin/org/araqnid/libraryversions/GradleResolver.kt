package org.araqnid.libraryversions

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

object GradleResolver : Resolver {
    private const val url = "https://gradle.org/releases/"
    private val versionPattern = Regex("""<a name="([0-9]\.[0-9.]+)">""")

    override fun findVersions(httpClient: HttpClient) = flow {
        val request = HttpRequest.newBuilder().uri(URI(url)).build()
        val response = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofPublisher())
                .await()
                .also { verifyOk(request, it) }

        emitAll(response.body().asFlow().flatMapConcat { it.asFlow() }.decodeText().splitByLines().extractVersions().take(3))
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

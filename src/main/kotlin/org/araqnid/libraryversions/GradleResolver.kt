package org.araqnid.libraryversions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.future.await
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

object GradleResolver : Resolver {
    private const val url = "https://gradle.org/releases/"
    private val versionPattern = Regex("""<a name="([0-9]\.[0-9.]+)">""")

    override fun findVersions(httpClient: HttpClient): Flow<String> {
        return flow {
            val request = HttpRequest.newBuilder().uri(URI(url)).build()
            val response = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .await()
                    .also { verifyOk(request, it) }

            sequence {
                var lastPrefix: String? = null
                for (input in versionPattern.findAll(response.body()).map {
                    parseVersion(it.groupValues[1])
                }) {
                    val versionPrefix = "${input.parts[0].number}.${input.parts[1].number}"
                    if (lastPrefix == null || lastPrefix != versionPrefix) {
                        yield(input)
                        lastPrefix = versionPrefix
                    }
                }
            }.take(3).forEach { emit(it.string) }
        }
    }

    override fun toString() = "Gradle"
}

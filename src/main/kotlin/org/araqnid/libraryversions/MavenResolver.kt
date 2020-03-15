package org.araqnid.libraryversions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.future.await
import nu.xom.Builder
import java.io.ByteArrayInputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class MavenResolver(
    repoUrl: String,
    private val artifactGroupId: String,
    private val artifactId: String,
    private val filters: List<Regex>
) : Resolver {
    private val url = "$repoUrl/${artifactGroupId.replace('.', '/')}/$artifactId/maven-metadata.xml"

    override fun findVersions(httpClient: HttpClient): Flow<String> {
        return when (filters.size) {
            0 -> {
                flow {
                    fetchVersionStrings(httpClient).maxBy { parseVersion(it) }?.let { emit(it) }
                }
            }
            1 -> {
                val filter = filters.single()
                flow {
                    fetchVersionStrings(httpClient)
                        .filter { filter.containsMatchIn(it) }
                        .maxBy { parseVersion(it) }
                        ?.let { emit(it) }
                }
            }
            else -> {
                flow {
                    val latestVersions = arrayOfNulls<Version>(filters.size)
                    for (versionString in fetchVersionStrings(httpClient)) {
                        for ((index, filter) in filters.withIndex()) {
                            val version by lazy(mode = LazyThreadSafetyMode.NONE) { parseVersion(versionString) }
                            if (filter.containsMatchIn(versionString)) {
                                latestVersions[index] = latestVersions[index]?.coerceAtLeast(version) ?: version
                            }
                        }
                    }
                    for (version in latestVersions) {
                        version?.let { emit(it.string) }
                    }
                }
            }
        }
    }

    private suspend fun fetchVersionStrings(httpClient: HttpClient): Collection<String> {
        val request = HttpRequest.newBuilder(URI(url)).build()
        val response = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
            .await()
            .also { verifyOk(request, it) }
        return Builder().build(ByteArrayInputStream(response.body()))
            .query("/metadata/versioning/versions/version")
            .map { it.value ?: "" }
    }

    override fun toString(): String {
        return if (filters.isNotEmpty()) {
            val combinedFilter = Regex(filters.joinToString("|") { it.pattern })
            "Maven: $artifactGroupId:$artifactId =~ /${combinedFilter.pattern}/"
        } else
            "Maven: $artifactGroupId:$artifactId"
    }
}

fun mavenCentral(artifactGroupId: String, artifactId: String, vararg filters: Regex): MavenResolver =
    MavenResolver(
        "https://repo.maven.apache.org/maven2",
        artifactGroupId,
        artifactId,
        filters.toList()
    )

fun jcenter(artifactGroupId: String, artifactId: String, vararg filters: Regex): MavenResolver =
    MavenResolver(
        "https://jcenter.bintray.com",
        artifactGroupId,
        artifactId,
        filters.toList()
    )

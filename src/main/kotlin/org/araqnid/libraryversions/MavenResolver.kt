package org.araqnid.libraryversions

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

    override fun findVersions(httpClient: HttpClient) = flow {
        val request = HttpRequest.newBuilder(URI(url)).build()
        val response = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
            .await()
            .also { verifyOk(request, it) }
        extractLatestMavenVersions(
            Builder().build(ByteArrayInputStream(response.body()))
                .query("/metadata/versioning/versions/version")
                .map { it.value ?: "" },
            filters
        ).forEach {
            emit(it)
        }
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

private fun extractLatestMavenVersions(
    strings: Collection<String>,
    filters: List<Regex>
): List<String> {
    if (filters.isEmpty()) {
        val latestVersion = strings.maxBy { parseVersion(it) }
        return if (latestVersion != null)
            listOf(latestVersion)
        else
            emptyList()
    } else {
        val filterOutputs = arrayOfNulls<Version>(filters.size)

        for (string in strings) {
            val version = parseVersion(string)
            for (i in filters.indices) {
                if (filters[i].containsMatchIn(string)) {
                    val latestVersionSeen = filterOutputs[i]
                    if (latestVersionSeen == null)
                        filterOutputs[i] = version
                    else
                        filterOutputs[i] = latestVersionSeen.coerceAtLeast(version)
                }
            }
        }

        return sequence {
            for (i in filters.indices) {
                val latestVersion = filterOutputs[i]
                if (latestVersion != null)
                    yield(latestVersion.string)
            }
        }.toList()
    }
}

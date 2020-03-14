package org.araqnid.libraryversions

import kotlinx.coroutines.flow.flow

class MavenResolver(repoUrl: String,
                    private val artifactGroupId: String,
                    private val artifactId: String,
                    private val filters: List<Regex>) : Resolver {
    private val url = "$repoUrl/${artifactGroupId.replace('.', '/')}/$artifactId/maven-metadata.xml"

    override fun findVersions(httpFetcher: HttpFetcher) = flow {
        extractLatestMavenVersions(fetchMavenVersionsFromMetadata(url, httpFetcher), filters).forEach {
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
        MavenResolver("https://repo.maven.apache.org/maven2",
                artifactGroupId,
                artifactId,
                filters.toList())

fun jcenter(artifactGroupId: String, artifactId: String, vararg filters: Regex): MavenResolver =
        MavenResolver("https://jcenter.bintray.com",
                artifactGroupId,
                artifactId,
                filters.toList())

internal expect suspend fun fetchMavenVersionsFromMetadata(url: String, httpFetcher: HttpFetcher): Collection<String>

private fun extractLatestMavenVersions(strings: Collection<String>,
                                        filters: List<Regex>): List<String> {
    if (filters.isEmpty()) {
        val latestVersion = strings.maxBy { parseVersion(it) }
        if (latestVersion != null)
            return listOf(latestVersion)
        else
            return emptyList()
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

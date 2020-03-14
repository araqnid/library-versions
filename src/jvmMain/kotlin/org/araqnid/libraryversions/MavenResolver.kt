package org.araqnid.libraryversions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import nu.xom.Builder
import java.io.ByteArrayInputStream

actual class MavenResolver actual constructor(repoUrl: String,
                                              private val artifactGroupId: String,
                                              private val artifactId: String,
                                              private val filters: List<Regex>) : Resolver {
    private val url = mavenMetadataUrl(repoUrl, artifactGroupId, artifactId)

    override fun findVersions(httpFetcher: HttpFetcher): Flow<String> {
        return flow {
            val response = httpFetcher.getBinary(url)

            val builder = Builder()
            val doc = builder.build(ByteArrayInputStream(response.data))
            val strings = doc.query("/metadata/versioning/versions/version").map { it.value ?: "" }
            extractLatestMavenVersions(strings, filters).forEach {
                emit(it)
            }
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

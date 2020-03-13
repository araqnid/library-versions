package org.araqnid.libraryversions

import kotlinx.coroutines.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

actual class MavenResolver actual constructor(repoUrl: String,
                                              private val artifactGroupId: String,
                                              private val artifactId: String,
                                              private val filters: List<Regex>) : Resolver {
    private val requestURI = "$repoUrl/${artifactGroupId.replace('.', '/')}/$artifactId/maven-metadata.xml"

    override fun findVersions(httpFetcher: HttpFetcher): Flow<String> {
        return flow {
            val response = httpFetcher.getText(requestURI)

            val strings = parseStringPromise(response.data).await().metadata.versioning[0].versions[0].version.unsafeCast<Array<String>>()
            extractLatestMavenVersions(strings.toList(),
                    filters).forEach { emit(it) }
        }
    }

    override fun toString(): String {
        return if (filters.isNotEmpty()) {
            val combinedFilter = filters.joinToString("|") { it.pattern }
            "Maven: $artifactGroupId:$artifactId =~ /$combinedFilter/"
        } else
            "Maven: $artifactGroupId:$artifactId"
    }
}

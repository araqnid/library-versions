package org.araqnid.libraryversions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import libxml2.xmlReadMemory

actual class MavenResolver actual constructor(repoUrl: String,
                                              private val artifactGroupId: String,
                                              private val artifactId: String,
                                              private val filters: List<Regex>) : Resolver {
    private val requestURI = mavenMetadataUrl(repoUrl, artifactGroupId, artifactId)

    override fun findVersions(httpFetcher: HttpFetcher): Flow<String> = flow {
        val response = httpFetcher.getText(requestURI)

        val content = response.data
        val versions = xmlReadMemory(content, content.length, null, "utf-8", 0)!!.use { doc ->
            doc.rootElement
                    .firstChild("versioning")
                    ?.firstChild("versions")
                    ?.children()
                    ?.mapNotNull { node ->
                        if (node.isElement && node.name == "version") {
                            node.text()
                        }
                        else {
                            null
                        }
                    } ?: emptyList()
        }

        extractLatestMavenVersions(versions, filters).forEach { emit(it) }
    }

    override fun toString(): String {
        return if (filters.isNotEmpty()) {
            val combinedFilter = Regex(filters.joinToString("|") { it.pattern })
            "Maven: $artifactGroupId:$artifactId =~ /${combinedFilter.pattern}/"
        } else
            "Maven: $artifactGroupId:$artifactId"
    }
}

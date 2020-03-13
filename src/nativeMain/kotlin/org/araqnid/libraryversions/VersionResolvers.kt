package org.araqnid.libraryversions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

actual object ZuluResolver : Resolver {
    override fun findVersions(httpFetcher: HttpFetcher): Flow<String> = emptyFlow()
}

actual object NodeJsResolver : Resolver {
    override fun findVersions(httpFetcher: HttpFetcher): Flow<String> = emptyFlow()
}

actual class MavenResolver actual constructor(repoUrl: String,
                                              artifactGroupId: String,
                                              artifactId: String,
                                              filters: List<Regex>) : Resolver {
    override fun findVersions(httpFetcher: HttpFetcher): Flow<String> = emptyFlow()
}

internal actual suspend fun readTextFile(filename: String): String = TODO()

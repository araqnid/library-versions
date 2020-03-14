package org.araqnid.libraryversions

import nu.xom.Builder
import java.io.ByteArrayInputStream

internal actual suspend fun fetchMavenVersionsFromMetadata(url: String, httpFetcher: HttpFetcher): Collection<String> {
    return Builder().build(ByteArrayInputStream(httpFetcher.getBinary(url).data))
            .query("/metadata/versioning/versions/version")
            .map { it.value ?: "" }
}

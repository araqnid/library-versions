package org.araqnid.libraryversions

import kotlinx.coroutines.await

internal actual suspend fun fetchMavenVersionsFromMetadata(url: String, httpFetcher: HttpFetcher): Collection<String> {
    return parseStringPromise(httpFetcher.getText(url).data).await()
            .metadata.versioning[0].versions[0].version
            .unsafeCast<Array<String>>().toList()
}

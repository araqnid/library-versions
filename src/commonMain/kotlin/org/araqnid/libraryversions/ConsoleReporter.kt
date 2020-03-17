package org.araqnid.libraryversions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList

suspend fun showVersionsOnConsole(resolvers: Collection<Resolver>, httpFetcher: HttpFetcher) {
    println("Latest Versions")
    println("===============")
    println("")

    resolvers.map { resolver ->
            resolver.findVersions(httpFetcher)
                .flowOn(Dispatchers.Default)
                .map { version -> resolver to version }
                .catch { ex ->
                    println("- $resolver")
                    println("  FAILED: $ex")
                }
        }
        .asFlow()
        .flattenMerge()
        .toList()
        .sortedBy { (resolver, _) -> resolver.toString() }
        .forEach { (resolver, version) ->
            println("- $resolver")
            println("  $version")
        }
}

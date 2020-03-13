package org.araqnid.libraryversions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking

fun main(argv: Array<String>) {
    runBlocking {
        println("Latest Versions")
        println("===============")
        println("")

        setOf(GradleResolver).map { resolver ->
                    resolver.findVersions(CurlHttpFetcher)
                            .flowOn(Dispatchers.Default)
                            .map { version -> resolver to version }
                            .catch { ex ->
                                println("- $resolver")
                                ex.printStackTrace()
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
}

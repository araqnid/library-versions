package org.araqnid.libraryversions.js

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import org.araqnid.libraryversions.js.axios.Axios

fun findLatestVersions(): Job {
    return GlobalScope.launch {
        println("Latest Versions")
        println("===============")
        println("")

        versionResolvers.map { resolver ->
                    resolver.findVersions(Axios)
                            .map { version -> resolver to version }
                            .catch { ex ->
                                println("- $resolver")
                                console.log(ex)
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

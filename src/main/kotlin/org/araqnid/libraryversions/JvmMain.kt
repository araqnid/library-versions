package org.araqnid.libraryversions

import com.google.common.base.Throwables
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import java.net.http.HttpClient

@OptIn(FlowPreview::class)
fun main(args: Array<String>) {
    runBlocking {
        println("Latest Versions")
        println("===============")
        println("")

        loadVersionResolvers(if (args.isNotEmpty()) args[0] else null).map { resolver ->
                    resolver.findVersions(HttpClient.newHttpClient())
                            .flowOn(Dispatchers.Default)
                            .map { version -> resolver to version }
                            .catch { ex ->
                                println("- $resolver")
                                Throwables.getStackTraceAsString(ex).lines().map { "  $it" }.forEach { println(it) }
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

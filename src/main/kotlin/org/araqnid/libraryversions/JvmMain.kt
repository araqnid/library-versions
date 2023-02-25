package org.araqnid.libraryversions

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.toList
import org.araqnid.kotlin.argv.ArgParser
import org.araqnid.kotlin.argv.ArgType
import org.araqnid.kotlin.argv.parseArgv
import java.net.http.HttpClient
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val configFile = ArgParser("library-versions").run {
        val configFile by argument(ArgType.STRING, "Artifact list").optional()
        if (!parseArgv(args)) {
            println("Syntax: library-versions ${buildSyntax()}")
            exitProcess(1)
        }
        configFile
    }

    runBlocking {
        println("Latest Versions")
        println("===============")
        println("")

        val httpClient = HttpClient.newHttpClient()

        val resolved = mutableMapOf<String, Deferred<Result<List<String>>>>()

        for (resolver in loadVersionResolvers(configFile)) {
            resolved[resolver.toString()] = async(Dispatchers.Default + CoroutineName(resolver.toString())) {
                try {
                    Result.success(resolver.findVersions(httpClient).toList())
                } catch (ex: RuntimeException) {
                    Result.failure(ex)
                }
            }
        }

        for ((resolver, deferredVersions) in resolved.entries.sortedBy { (resolver, _) -> resolver }) {
            deferredVersions.await().fold(
                { versions ->
                    for (version in versions) {
                        println("- $resolver")
                        println("  $version")
                    }
                },
                { ex ->
                    println("! $resolver")
                    println("! $ex")
                }
            )
        }
    }
}

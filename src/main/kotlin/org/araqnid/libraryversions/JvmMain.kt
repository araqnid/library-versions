package org.araqnid.libraryversions

import com.google.common.base.Throwables
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.araqnid.kotlin.argv.ArgParser
import org.araqnid.kotlin.argv.ArgType
import org.araqnid.kotlin.argv.parseArgv
import java.net.http.HttpClient
import kotlin.system.exitProcess

@OptIn(FlowPreview::class)
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

        loadVersionResolvers(configFile).map { resolver ->
                resolver.findVersions(httpClient)
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

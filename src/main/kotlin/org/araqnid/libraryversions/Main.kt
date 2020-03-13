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
import java.io.File
import java.net.URI
import java.net.http.HttpClient

object Main {
    @JvmStatic
    @OptIn(FlowPreview::class)
    fun main(args: Array<String>) {
        val httpClient = HttpClient.newHttpClient()
        runBlocking {
            println("Latest Versions")
            println("===============")
            println("")

            loadVersionResolvers(if (args.isNotEmpty()) args[0] else null).map { resolver ->
                        resolver.findVersions(httpClient)
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

    private fun loadVersionResolvers(configFile: String?): Collection<Resolver> {
        if (configFile == null) return defaultVersionResolvers
        return File(configFile).reader().useLines { lines ->
            lines.filterNot { it.isBlank() }.map(::configureResolver).toList()
        }
    }

    private fun configureResolver(configLine: String): Resolver {
        val words = configLine.split(Regex("""\s+"""))
        return when (words[0]) {
            "Gradle" -> GradleResolver
            "Zulu" -> ZuluResolver
            "mavenCentral" -> configureMavenResolver(words.drop(1)) { artifactGroupId, artifactId, filters -> mavenCentral(artifactGroupId, artifactId, *filters.toTypedArray()) }
            "jcenter" -> configureMavenResolver(words.drop(1))  { artifactGroupId, artifactId, filters -> jcenter(artifactGroupId, artifactId, *filters.toTypedArray()) }
            "Maven" -> configureMavenResolver(words.drop(2)) { artifactGroupId, artifactId, filters -> MavenResolver(URI(words[1]), artifactGroupId, artifactId, filters) }
            else -> error("Unrecognised resolver type: ${words[0]}")
        }
    }

    private fun configureMavenResolver(words: List<String>, factory: (String, String, List<Regex>) -> MavenResolver): MavenResolver {
        val (artifactGroupId, artifactId) = words[0].split(':', limit = 2)
        val filters = words.drop(1).map { str ->
            check(str.startsWith("/") && str.endsWith("/"))
            Regex(str.drop(1).dropLast(1))
        }
        return factory(artifactGroupId, artifactId, filters)
    }
}

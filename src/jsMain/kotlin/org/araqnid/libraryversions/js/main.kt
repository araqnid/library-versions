package org.araqnid.libraryversions.js

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.araqnid.libraryversions.js.axios.Axios
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Suppress("unused")
@JsName("findLatestVersions")
fun findLatestVersions(configFile: String? = null): Job {
    return GlobalScope.launch {
        println("Latest Versions")
        println("===============")
        println("")

        loadVersionResolvers(configFile).map { resolver ->
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

@JsModule("fs")
external object FS {
    fun readFile(path: String, encoding: String, callback: (Throwable?, String?) -> Unit)
}

private suspend fun readTextFile(path: String, encoding: String = "utf-8"): String {
    return suspendCancellableCoroutine { cont ->
        FS.readFile(path, encoding) { err, data ->
            if (err == null)
                cont.resume(data!!)
            else
                cont.resumeWithException(err)
        }
    }
}

private suspend fun loadVersionResolvers(configFile: String?): Collection<Resolver> {
    if (configFile == null) return defaultVersionResolvers
    return readTextFile(configFile).lineSequence().filterNot { it.isBlank() }.filterNot { it == "Zulu" }.map(::configureResolver).toList()
}

private fun configureResolver(configLine: String): Resolver {
    val words = configLine.split(Regex("""\s+"""))
    return when (words[0]) {
        "Gradle" -> GradleResolver
        "NodeJs" -> NodeJsResolver
        "mavenCentral" -> configureMavenResolver(words.drop(1)) { artifactGroupId, artifactId, filters -> mavenCentral(artifactGroupId, artifactId, *filters.toTypedArray()) }
        "jcenter" -> configureMavenResolver(words.drop(1))  { artifactGroupId, artifactId, filters -> jcenter(artifactGroupId, artifactId, *filters.toTypedArray()) }
        "Maven" -> configureMavenResolver(words.drop(2)) { artifactGroupId, artifactId, filters -> MavenResolver(words[1], artifactGroupId, artifactId, filters) }
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

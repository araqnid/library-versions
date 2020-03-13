package org.araqnid.libraryversions

import kotlinx.coroutines.flow.Flow

interface Resolver {
    fun findVersions(httpFetcher: HttpFetcher): Flow<String>
}

interface HttpFetcher {
    suspend fun getText(url: String): Response<String>
    suspend fun getBinary(url: String): Response<ByteArray>

    data class Response<out T>(val statusCode: Int, val data: T)
}

val defaultVersionResolvers = listOf(
        mavenCentral("org.jetbrains.kotlinx",
                "kotlinx-coroutines-core"),
        mavenCentral("org.eclipse.jetty", "jetty-server",
                Regex("""^9""")),
        mavenCentral("com.google.guava", "guava"),
        mavenCentral("com.fasterxml.jackson.core", "jackson-core"),
        mavenCentral("com.google.inject", "guice"),
        mavenCentral("org.slf4j", "slf4j-api",
                Regex("""^1\.7""")),
        mavenCentral("ch.qos.logback", "logback-classic",
                Regex("""^1\.2""")),
        mavenCentral("com.google.protobuf", "protobuf-java"),
        mavenCentral("org.jboss.resteasy", "resteasy-jaxrs",
                Regex("""^3""")),
        mavenCentral("org.scala-lang", "scala-library",
                Regex("""^2\.13"""),
                Regex("""^2\.12"""),
                Regex("""^2\.11""")),
        mavenCentral("org.jetbrains.kotlin", "kotlin-stdlib"),
        jcenter("org.jetbrains.kotlinx", "kotlinx-html-assembly"),
        jcenter("com.natpryce", "hamkrest"),
        GradleResolver,
        ZuluResolver,
        NodeJsResolver
)

expect object ZuluResolver : Resolver
expect object NodeJsResolver : Resolver

suspend fun loadVersionResolvers(configFile: String?): Collection<Resolver> {
    if (configFile == null) return defaultVersionResolvers
    return readTextFile(configFile).lineSequence().filterNot { it.isBlank() }.map(::configureResolver).toList()
}

internal expect suspend fun readTextFile(filename: String): String

private fun configureResolver(configLine: String): Resolver {
    val words = configLine.split(Regex("""\s+"""))
    return when (words[0]) {
        "Gradle" -> GradleResolver
        "NodeJs" -> NodeJsResolver
        "Zulu" -> ZuluResolver
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

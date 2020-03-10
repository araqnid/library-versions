package org.araqnid.libraryversions.js

import kotlinx.coroutines.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.araqnid.libraryversions.js.axios.AxiosClient
import org.araqnid.libraryversions.js.axios.getText

val versionResolvers = listOf(
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
        GradleResolver
)

interface Resolver {
    fun findVersions(axios: AxiosClient): Flow<String>
}

fun mavenCentral(artifactGroupId: String, artifactId: String, vararg filters: Regex): MavenResolver =
        MavenResolver("https://repo.maven.apache.org/maven2",
                artifactGroupId,
                artifactId,
                filters.toList())

fun jcenter(artifactGroupId: String, artifactId: String, vararg filters: Regex): MavenResolver =
        MavenResolver("https://jcenter.bintray.com",
                artifactGroupId,
                artifactId,
                filters.toList())

class MavenResolver(repoUrl : String,
                    private val artifactGroupId: String,
                    private val artifactId: String,
                    private val filters: List<Regex>) : Resolver {
    private val requestURI = "$repoUrl/${artifactGroupId.replace('.', '/')}/$artifactId/maven-metadata.xml"

    override fun findVersions(axios: AxiosClient): Flow<String> {
        return flow<String> {
            val response = axios.getText(requestURI)
            check(response.status == 200) { "$requestURI: ${response.status} ${response.statusText}" }
            println("$requestURI: ${response.status} ${response.statusText}")
            val strings = parseStringPromise(response.data).await().metadata.versioning[0].versions[0].version.unsafeCast<Array<String>>()
            if (filters.isEmpty()) {
                val latestVersion = strings.maxBy { parseVersion(it) }
                if (latestVersion != null)
                    emit(latestVersion)
            }
            else {
                val filterOutputs = arrayOfNulls<Version>(filters.size)

                for (string in strings) {
                    val version = parseVersion(string)
                    for (i in filters.indices) {
                        if (filters[i].containsMatchIn(string)) {
                            val latestVersionSeen = filterOutputs[i]
                            if (latestVersionSeen == null)
                                filterOutputs[i] = version
                            else
                                filterOutputs[i] = latestVersionSeen.coerceAtLeast(version)
                        }
                    }
                }

                for (i in filters.indices) {
                    val latestVersion = filterOutputs[i]
                    if (latestVersion != null)
                        emit(latestVersion.string)
                }
            }
        }
    }

    override fun toString(): String {
        return if (filters.isNotEmpty()) {
            val combinedFilter = filters.joinToString("|") { it.pattern }
            "Maven: $artifactGroupId:$artifactId =~ /$combinedFilter/"
        }
        else
            "Maven: $artifactGroupId:$artifactId"
    }
}

object GradleResolver : Resolver {
    private const val requestURI = "https://gradle.org/releases/"
    private val versionPattern = Regex("""<a name="([0-9]\.[0-9.]+)">""")

    override fun findVersions(axios: AxiosClient): Flow<String> {
        return flow {
            val response = axios.getText(requestURI)
            check(response.status == 200) { "$requestURI: ${response.status} ${response.statusText}" }
            println("$requestURI: ${response.status} ${response.statusText}")

            val responseText: String = response.data
            sequence {
                var lastPrefix: String? = null
                for (input in versionPattern.findAll(responseText).map {
                    parseVersion(it.groupValues[1])
                }) {
                    val versionPrefix = "${input.parts[0].number}.${input.parts[1].number}"
                    if (lastPrefix == null || lastPrefix != versionPrefix) {
                        yield(input)
                        lastPrefix = versionPrefix
                    }
                }
            }.take(3).forEach { emit(it.string) }
        }
    }

    override fun toString(): String = "Gradle"
}

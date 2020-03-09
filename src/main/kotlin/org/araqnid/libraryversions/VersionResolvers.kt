package org.araqnid.libraryversions

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.future.await
import nu.xom.Builder
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers

val versionResolvers = listOf(
        mavenCentral("org.jetbrains.kotlinx", "kotlinx-coroutines-core"),
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
        ZuluResolver
)

private val logger = LoggerFactory.getLogger("org.araqnid.espider.libraryversionsdashboard.VersionResolvers")

interface Resolver {
    fun findVersions(httpClient: HttpClient): Flow<String>
}

fun mavenCentral(artifactGroupId: String, artifactId: String, vararg filters: Regex): MavenResolver =
        MavenResolver(URI("https://repo.maven.apache.org/maven2"),
                artifactGroupId,
                artifactId,
                filters.toList())

fun jcenter(artifactGroupId: String, artifactId: String, vararg filters: Regex): MavenResolver =
        MavenResolver(URI("https://jcenter.bintray.com"),
                artifactGroupId,
                artifactId,
                filters.toList())

class MavenResolver(repoUrl : URI,
                    private val artifactGroupId: String,
                    private val artifactId: String,
                    private val filters: List<Regex>) : Resolver {
    private val request = HttpRequest.newBuilder()
            .uri(URI("$repoUrl/${artifactGroupId.replace('.', '/')}/$artifactId/maven-metadata.xml"))
            .build()

    override fun findVersions(httpClient: HttpClient): Flow<String> {
        return flow<String> {
            val response = httpClient.sendAsync(request, BodyHandlers.ofByteArray()).await()
            check(response.statusCode() == 200) { "${request.uri()}: ${response.statusCode()} " }
            logger.info("${request.uri()}: ${response.statusCode()}")

            val builder = Builder()
            val doc = builder.build(ByteArrayInputStream(response.body()))
            val strings = doc.query("/metadata/versioning/versions/version").map { it.value ?: "" }
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
            val combinedFilter = Regex(filters.joinToString("|") { it.pattern })
            "Maven: $artifactGroupId:$artifactId =~ /${combinedFilter.pattern}/"
        }
        else
            "Maven: $artifactGroupId:$artifactId"
    }
}

object GradleResolver : Resolver {
    private val request = HttpRequest.newBuilder()
            .uri(URI("https://gradle.org/releases/"))
            .build()
    private val versionPattern = Regex("""<a name="([0-9]\.[0-9.]+)">""")

    override fun findVersions(httpClient: HttpClient): Flow<String> {
        return flow {
            val response = httpClient.sendAsync(request, BodyHandlers.ofString()).await()
            check(response.statusCode() == 200) { "${request.uri()}: ${response.statusCode()} " }
            logger.info("${request.uri()}: ${response.statusCode()}")

            val responseText: String = response.body()
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

object ZuluResolver : Resolver {
    private val packagesUrl = URI("http://repos.azulsystems.com/debian/dists/stable/main/binary-amd64/Packages.gz")
    private val request = HttpRequest.newBuilder()
            .uri(packagesUrl)
            .build()
    private val packagesPattern = Regex("""^zulu-(8|1[13-9])""")

    @OptIn(FlowPreview::class)
    override fun findVersions(httpClient: HttpClient): Flow<String> {
        return flow {
            val response = httpClient.sendAsync(request, BodyHandlers.ofPublisher()).await()
            check(response.statusCode() == 200) { "${request.uri()}: ${response.statusCode()} " }
            logger.info("${request.uri()}: ${response.statusCode()}")

            val pattern = Regex("""([A-Za-z0-9-]+): (.*)""")
            val packageFields = mutableMapOf<String, String>()
            val versionsForPackages = mutableMapOf<String, MutableList<Version>>()
            response.body().asFlow().flatMapConcat { it.asFlow() }.gunzip().decodeText().splitByLines().collect { line ->
                if (line.isEmpty()) {
                    val packageName = packageFields["package"] ?: throw IllegalArgumentException("No 'Package' in package")
                    val packageVersion = packageFields["version"] ?: throw IllegalArgumentException("No 'Version' in package")
                    if (packagesPattern.find(packageName) != null)
                        versionsForPackages.getOrPut(packageName) { mutableListOf<Version>() } += parseVersion(
                                packageVersion)
                    packageFields.clear()
                } else if (!line.startsWith(' ')) {
                    val match = pattern.matchEntire(line)
                            ?: throw IllegalArgumentException("Invalid packages line: $line")
                    val (name, value) = match.destructured
                    packageFields[name.toLowerCase()] = value
                }
            }
            for ((packageName, versions) in versionsForPackages) {
                val latestVersion = versions.max()!!
                emit(packageName + " " + latestVersion.string)
            }
        }
    }

    override fun toString(): String = "Zulu"
}
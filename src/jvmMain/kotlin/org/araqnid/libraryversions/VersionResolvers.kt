@file:JvmName("VersionResolversJVM")
package org.araqnid.libraryversions

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdNodeBasedDeserializer
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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

private val logger = LoggerFactory.getLogger("org.araqnid.libraryversions.VersionResolvers")

actual interface Resolver {
    fun findVersions(httpClient: HttpClient): Flow<String>
}

actual class MavenResolver actual constructor(repoUrl: String,
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
            } else {
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
        } else
            "Maven: $artifactGroupId:$artifactId"
    }
}

actual object GradleResolver : Resolver {
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

actual object ZuluResolver : Resolver {
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
                    val packageName = packageFields["package"] ?: error("No 'Package' in package")
                    val packageVersion = packageFields["version"] ?: error("No 'Version' in package")
                    if (packagesPattern.find(packageName) != null)
                        versionsForPackages.getOrPut(packageName) { mutableListOf<Version>() } += parseVersion(
                                packageVersion)
                    packageFields.clear()
                } else if (!line.startsWith(' ')) {
                    val match = pattern.matchEntire(line) ?: error("Invalid packages line: $line")
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

actual object NodeJsResolver : Resolver {
    private val url = URI("https://nodejs.org/dist/index.json")
    private val request = HttpRequest.newBuilder()
            .uri(url)
            .build()
    private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())

    override fun findVersions(httpClient: HttpClient): Flow<String> {
        return flow {
            val response = httpClient.sendAsync(request, BodyHandlers.ofByteArray()).await()
            check(response.statusCode() == 200) { "${request.uri()}: ${response.statusCode()} " }
            logger.info("${request.uri()}: ${response.statusCode()}")

            val (ltsVersions, nonLtsVersions) = objectMapper.readValue<List<Release>>(response.body()).partition { it.lts != null }
            ltsVersions.maxBy { it.parsedVersion }?.let { emit("${it.version} ${it.lts}") }
            nonLtsVersions.maxBy { it.parsedVersion }?.let { emit(it.version) }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Release(
            val version: String,
            @JsonDeserialize(using = LTSVersionDeserializer::class)
            val lts: String?,
            val security: Boolean
    ) {
        val parsedVersion by lazy { parseVersion(version) }
    }

    class LTSVersionDeserializer : StdNodeBasedDeserializer<String>(String::class.java) {
        override fun convert(root: JsonNode, ctxt: DeserializationContext): String? {
            return if (root.isBoolean)
                null
            else
                root.asText()
        }
    }

    override fun toString(): String = "NodeJs"
}

@Suppress("RedundantSuspendModifier")
internal actual suspend fun readTextFile(filename: String): String = filename.reader().readText()

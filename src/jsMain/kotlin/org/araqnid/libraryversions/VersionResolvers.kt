package org.araqnid.libraryversions

import kotlinx.coroutines.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.suspendCancellableCoroutine
import org.araqnid.libraryversions.js.axios.getJson
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual class MavenResolver actual constructor(repoUrl: String,
                                              private val artifactGroupId: String,
                                              private val artifactId: String,
                                              private val filters: List<Regex>) : Resolver {
    private val requestURI = "$repoUrl/${artifactGroupId.replace('.', '/')}/$artifactId/maven-metadata.xml"

    override fun findVersions(httpFetcher: HttpFetcher): Flow<String> {
        return flow<String> {
            val response = httpFetcher.getText(requestURI)

            val strings = parseStringPromise(response.data).await().metadata.versioning[0].versions[0].version.unsafeCast<Array<String>>()
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
            val combinedFilter = filters.joinToString("|") { it.pattern }
            "Maven: $artifactGroupId:$artifactId =~ /$combinedFilter/"
        } else
            "Maven: $artifactGroupId:$artifactId"
    }
}

actual object NodeJsResolver : Resolver {
    private const val requestURI = "https://nodejs.org/dist/index.json"

    override fun findVersions(httpFetcher: HttpFetcher): Flow<String> {
        return flow {
            val response = (httpFetcher as AxiosHttpFetcher).axios.getJson<Array<NodeJsReleaseJson>>(requestURI)
            check(response.status == 200) { "${requestURI}: ${response.status} ${response.statusText}" }
            println("${requestURI}: ${response.status} ${response.statusText}")

            val (ltsVersions, nonLtsVersions) = response.data.asSequence().map { it.toRelease() }.partition { it.lts != null }
            ltsVersions.maxBy { it.parsedVersion }?.let { emit("${it.version} ${it.lts}") }
            nonLtsVersions.maxBy { it.parsedVersion }?.let { emit(it.version) }
        }
    }

    private fun NodeJsReleaseJson.toRelease(): Release = Release(
            version = version,
            lts = if (lts == false) null else lts.unsafeCast<String>(),
            security = security
    )

    data class Release(
            val version: String,
            val lts: String?,
            val security: Boolean
    ) {
        val parsedVersion by lazy { parseVersion(version) }
    }

    override fun toString(): String = "NodeJs"
}

private external interface NodeJsReleaseJson {
    val version: String
    val lts: Any // string | boolean
    val security: Boolean
}

@JsModule("fs")
private external object FS {
    fun readFile(path: String, encoding: String, callback: (Throwable?, String?) -> Unit)
}

internal actual suspend fun readTextFile(filename: String): String {
    return suspendCancellableCoroutine { cont ->
        FS.readFile(filename, "utf-8") { err, data ->
            if (err == null)
                cont.resume(data!!)
            else
                cont.resumeWithException(err)
        }
    }
}

actual object ZuluResolver : Resolver {
    override fun findVersions(httpFetcher: HttpFetcher): Flow<String> {
        return flowOf()
    }
}

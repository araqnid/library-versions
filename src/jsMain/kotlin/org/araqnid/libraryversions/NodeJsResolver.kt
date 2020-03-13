package org.araqnid.libraryversions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.araqnid.libraryversions.js.axios.getJson

actual object NodeJsResolver : Resolver {
    private const val requestURI = "https://nodejs.org/dist/index.json"

    override fun findVersions(httpFetcher: HttpFetcher): Flow<String> {
        return flow {
            val response = (httpFetcher as AxiosHttpFetcher).axios.getJson<Array<NodeJsReleaseJson>>(
                    requestURI)
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

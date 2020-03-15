package org.araqnid.libraryversions

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.future.await
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest

object ZuluResolver : Resolver {
    private val packagesUrl = URI("http://repos.azulsystems.com/debian/dists/stable/main/binary-amd64/Packages.gz")
    private val request = HttpRequest.newBuilder()
        .uri(packagesUrl)
        .build()
    private val packagesPattern = Regex("""^zulu-(8|1[13-9])""")

    @OptIn(FlowPreview::class)
    override fun findVersions(httpClient: HttpClient): Flow<String> {
        return flow {
            val response = httpClient.sendAsync(
                request,
                flowBodyHandler
            ).await().also { verifyOk(request, it) }

            val pattern = Regex("""([A-Za-z0-9-]+): (.*)""")
            val packageFields = mutableMapOf<String, String>()
            val versionsForPackages = mutableMapOf<String, MutableList<Version>>()
            response.body().gunzip().decodeText().splitByLines()
                .collect { line ->
                    if (line.isEmpty()) {
                        val packageName = packageFields["package"] ?: error("No 'Package' in package")
                        val packageVersion = packageFields["version"] ?: error("No 'Version' in package")
                        if (packagesPattern.find(packageName) != null)
                            versionsForPackages.getOrPut(packageName) { mutableListOf<Version>() } += parseVersion(
                                packageVersion
                            )
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

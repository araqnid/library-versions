package org.araqnid.libraryversions

expect class MavenResolver(repoUrl: String,
                           artifactGroupId: String,
                           artifactId: String,
                           filters: List<Regex>) : Resolver

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

internal fun extractLatestMavenVersions(strings: List<String>,
                                        filters: List<Regex>): List<String> {
    if (filters.isEmpty()) {
        val latestVersion = strings.maxBy { parseVersion(it) }
        if (latestVersion != null)
            return listOf(latestVersion)
        else
            return emptyList()
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

        return sequence {
            for (i in filters.indices) {
                val latestVersion = filterOutputs[i]
                if (latestVersion != null)
                    yield(latestVersion.string)
            }
        }.toList()
    }
}

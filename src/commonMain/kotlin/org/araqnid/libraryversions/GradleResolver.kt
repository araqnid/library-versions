package org.araqnid.libraryversions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

object GradleResolver : Resolver {
    private const val url = "https://gradle.org/releases/"
    private val versionPattern = Regex("""<a name="([0-9]\.[0-9.]+)">""")

    override fun findVersions(httpFetcher: HttpFetcher): Flow<String> {
        return flow {
            val response = httpFetcher.getText(url)

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
}

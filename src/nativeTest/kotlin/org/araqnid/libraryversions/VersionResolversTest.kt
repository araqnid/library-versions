package org.araqnid.libraryversions

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.araqnid.libraryversions.assertions.assertThat
import org.araqnid.libraryversions.assertions.equalTo
import org.araqnid.libraryversions.assertions.has
import kotlin.test.Test

class VersionResolversTest {
    @Test
    fun resolve_maven_central_artifact() {
        runBlocking {
            val result = mavenCentral("org.jetbrains.kotlinx",
                    "kotlinx-coroutines-core").findVersions(testHttpFetcher).toList()
            assertThat(result, has(Collection<*>::size, equalTo(1)))
        }
    }

    @Test
    fun resolve_gradle() {
        runBlocking {
            val result = GradleResolver.findVersions(testHttpFetcher).toList()
            assertThat(result, has(Collection<*>::size, equalTo(3)))
        }
    }

    companion object {
        val testHttpFetcher = CurlHttpFetcher
    }
}

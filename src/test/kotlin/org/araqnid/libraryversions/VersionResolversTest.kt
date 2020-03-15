package org.araqnid.libraryversions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.araqnid.libraryversions.assertions.anything
import org.araqnid.libraryversions.assertions.assertThat
import org.araqnid.libraryversions.assertions.contains
import org.araqnid.libraryversions.assertions.containsInAnyOrder
import org.araqnid.libraryversions.assertions.containsInOrder
import org.araqnid.libraryversions.assertions.greaterThan
import org.araqnid.libraryversions.assertions.has
import org.junit.Assume.assumeFalse
import org.junit.BeforeClass
import java.net.http.HttpClient
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test

class VersionResolversTest : CoroutineScope by CoroutineScope(EmptyCoroutineContext) {
    @Test
    fun resolve_maven_central_artifact() {
        val result = runBlocking {
            mavenCentral(
                "org.jetbrains.kotlinx",
                "kotlinx-coroutines-core"
            ).findVersions(httpClient).toList()
        }
        assertThat(result, containsInOrder(contains(Regex("""^1.3"""))))
    }

    @Test
    fun resolve_maven_central_artifact_with_single_filter() {
        val result = runBlocking {
            mavenCentral(
                "org.jetbrains.kotlinx",
                "kotlinx-coroutines-core",
                Regex("""^1.3""")
            ).findVersions(httpClient).toList()
        }
        assertThat(result, containsInAnyOrder(contains(Regex("""^1.3"""))))
    }

    @Test
    fun resolve_maven_central_artifact_with_multiple_filters() {
        val result = runBlocking {
            mavenCentral(
                "org.jetbrains.kotlinx",
                "kotlinx-coroutines-core",
                Regex("""^1.3"""),
                Regex("""^1.2""")
            ).findVersions(httpClient).toList()
        }
        assertThat(result, containsInAnyOrder(contains(Regex("""^1.3""")), contains(Regex("""^1.2"""))))
    }

    @Test
    fun resolve_gradle() {
        val result = runBlocking {
            GradleResolver.findVersions(httpClient).toList()
        }
        assertThat(result, containsInAnyOrder(anything, anything, anything))
    }

    @Test
    fun resolve_zulu() {
        val result = runBlocking {
            ZuluResolver.findVersions(httpClient).toList()
        }
        assertThat(result, has(Collection<*>::size, greaterThan(0)))
    }

    @Test
    fun resolve_nodejs() {
        val result = runBlocking {
            NodeJsResolver.findVersions(httpClient).toList()
        }
        assertThat(result, containsInAnyOrder(anything, anything))
    }

    companion object {
        val httpClient by lazy { HttpClient.newHttpClient() }

        @JvmStatic
        @BeforeClass
        fun allowDisablingInternetAccess() {
            val disableExternalIntegration = System.getenv("DISABLE_EXTERNAL_INTEGRRATION")?.toBoolean() ?: false
            assumeFalse(disableExternalIntegration)
        }
    }
}

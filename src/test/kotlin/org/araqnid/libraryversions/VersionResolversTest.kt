package org.araqnid.libraryversions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.araqnid.libraryversions.assertions.anything
import org.araqnid.libraryversions.assertions.assertThat
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
        runBlocking {
            val result = mavenCentral("org.jetbrains.kotlinx",
                    "kotlinx-coroutines-core").findVersions(httpClient).toList()
            assertThat(result, containsInOrder(anything))
        }
    }

    @Test
    fun resolve_gradle() {
        runBlocking {
            val result = GradleResolver.findVersions(httpClient).toList()
            assertThat(result, containsInOrder(anything, anything, anything))
        }
    }

    @Test
    fun resolve_zulu() {
        runBlocking {
            val result = ZuluResolver.findVersions(httpClient).toList()
            assertThat(result, has(Collection<*>::size, greaterThan(0)))
        }
    }

    @Test
    fun resolve_nodejs() {
        runBlocking {
            val result = NodeJsResolver.findVersions(httpClient).toList()
            assertThat(result, containsInOrder(anything, anything))
        }
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

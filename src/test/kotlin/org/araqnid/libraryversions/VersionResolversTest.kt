package org.araqnid.libraryversions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.araqnid.libraryversions.assertions.assertThat
import org.araqnid.libraryversions.assertions.equalTo
import org.araqnid.libraryversions.assertions.greaterThan
import org.araqnid.libraryversions.assertions.has
import java.net.http.HttpClient
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test

class VersionResolversTest : CoroutineScope by CoroutineScope(EmptyCoroutineContext) {
    @Test
    fun resolve_maven_central_artifact() {
        runBlocking {
            val result = mavenCentral("org.jetbrains.kotlinx",
                    "kotlinx-coroutines-core").findVersions(httpClient).toList()
            assertThat(result, has(Collection<*>::size, equalTo(1)))
        }
    }

    @Test
    fun resolve_gradle() {
        runBlocking {
            val result = GradleResolver.findVersions(httpClient).toList()
            assertThat(result, has(Collection<*>::size, equalTo(3)))
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
            assertThat(result, has(Collection<*>::size, equalTo(2)))
        }
    }

    companion object {
        val httpClient by lazy { HttpClient.newHttpClient() }
    }
}

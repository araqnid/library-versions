package org.araqnid.libraryversions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.promise
import org.araqnid.libraryversions.assertions.assertThat
import org.araqnid.libraryversions.assertions.equalTo
import org.araqnid.libraryversions.assertions.has
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.js.Promise
import kotlin.test.Test

class VersionResolversTest : CoroutineScope by CoroutineScope(EmptyCoroutineContext) {
    @Test
    fun resolve_maven_central_artifact(): Promise<*> = promise {
        val result = mavenCentral("org.jetbrains.kotlinx",
                "kotlinx-coroutines-core").findVersions(testHttpFetcher).toList()
        assertThat(result, has(Collection<*>::size, equalTo(1)))
    }

    @Test
    fun resolve_gradle(): Promise<*> = promise {
        val result = GradleResolver.findVersions(testHttpFetcher).toList()
        assertThat(result, has(Collection<*>::size, equalTo(3)))
    }

    @Test
    fun resolve_nodejs(): Promise<*> = promise {
        val result = NodeJsResolver.findVersions(testHttpFetcher).toList()
        assertThat(result, has(Collection<*>::size, equalTo(2)))
    }

    companion object {
        val testHttpFetcher = AxiosHttpFetcher()
    }
}
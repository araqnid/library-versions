package org.araqnid.libraryversions.js

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.promise
import org.araqnid.libraryversions.assertions.assertThat
import org.araqnid.libraryversions.assertions.equalTo
import org.araqnid.libraryversions.assertions.has
import org.araqnid.libraryversions.js.axios.Axios
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.js.Promise
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class VersionResolversTest : CoroutineScope by CoroutineScope(EmptyCoroutineContext) {

    @BeforeTest
    fun beforeTest() {
        println("before test")
    }

    @AfterTest
    fun afterTest() {
        println("after test")
    }

    @Test
    fun resolve_maven_central_artifact(): Promise<*> = promise {
        val result = mavenCentral("org.jetbrains.kotlinx",
                "kotlinx-coroutines-core").findVersions(Axios).toList()
        assertThat(result, has(Collection<*>::size, equalTo(1)))
    }

    @Test
    fun resolve_gradle(): Promise<*> = promise {
        val result = GradleResolver.findVersions(Axios).toList()
        assertThat(result, has(Collection<*>::size, equalTo(3)))
    }
}

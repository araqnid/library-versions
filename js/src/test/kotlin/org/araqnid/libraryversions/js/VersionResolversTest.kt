package org.araqnid.libraryversions.js

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.promise
import org.araqnid.libraryversions.js.axios.Axios
import kotlin.js.Promise
import kotlin.test.Test
import kotlin.test.assertEquals

class VersionResolversTest {
    @Test
    fun resolve_maven_central_artifact(): Promise<*> = GlobalScope.promise {
        val result = mavenCentral("org.jetbrains.kotlinx",
                "kotlinx-coroutines-core").findVersions(Axios).toList()
        assertEquals(1, result.size)
    }

    @Test
    fun resolve_gradle(): Promise<*> = GlobalScope.promise {
        val result = GradleResolver.findVersions(Axios).toList()
        assertEquals(3, result.size)
    }
}

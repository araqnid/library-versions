package org.araqnid.libraryversions.js

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.promise
import org.araqnid.libraryversions.js.assertions.assertThat
import org.araqnid.libraryversions.js.assertions.equalTo
import org.araqnid.libraryversions.js.assertions.has
import org.araqnid.libraryversions.js.axios.Axios
import kotlin.js.Promise
import kotlin.test.Test

class VersionResolversTest {
    @Test
    fun resolve_maven_central_artifact(): Promise<*> = GlobalScope.promise {
        val result = mavenCentral("org.jetbrains.kotlinx",
                "kotlinx-coroutines-core").findVersions(Axios).toList()
        assertThat(result, has(Collection<*>::size, equalTo(1)))
    }

    @Test
    fun resolve_gradle(): Promise<*> = GlobalScope.promise {
        val result = GradleResolver.findVersions(Axios).toList()
        assertThat(result, has(Collection<*>::size, equalTo(3)))
    }
}

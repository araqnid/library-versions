package org.araqnid.libraryversions

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import platform.posix.fclose
import platform.posix.fgets
import platform.posix.fopen

actual object ZuluResolver : Resolver {
    override fun findVersions(httpFetcher: HttpFetcher): Flow<String> = emptyFlow()
}

actual object NodeJsResolver : Resolver {
    override fun findVersions(httpFetcher: HttpFetcher): Flow<String> = emptyFlow()
}

actual class MavenResolver actual constructor(repoUrl: String,
                                              artifactGroupId: String,
                                              artifactId: String,
                                              filters: List<Regex>) : Resolver {
    override fun findVersions(httpFetcher: HttpFetcher): Flow<String> = emptyFlow()
}

@Suppress("RedundantSuspendModifier")
internal actual suspend fun <T> useLinesOfTextFile(filename: String, block: (Sequence<String>) -> T): T {
    val fh = fopen(filename, "r") ?: error("unable to open $filename")
    return try {
        block(generateSequence {
            memScoped {
                val buf = allocArray<ByteVar>(512)
                val got = fgets(buf, 512, fh)
                if (got != null)
                    buf.toKString().trimEnd()
                else
                    null
            }
        })
    } finally {
        fclose(fh);
    }
}

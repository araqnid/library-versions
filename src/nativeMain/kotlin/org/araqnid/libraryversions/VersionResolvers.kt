package org.araqnid.libraryversions

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.free
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.toKString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import platform.posix.fclose
import platform.posix.fgets
import platform.posix.fopen

actual object ZuluResolver : Resolver {
    override fun findVersions(httpFetcher: HttpFetcher): Flow<String> = emptyFlow()

    override fun toString() = "Zulu"
}

actual object NodeJsResolver : Resolver {
    override fun findVersions(httpFetcher: HttpFetcher): Flow<String> = emptyFlow()

    override fun toString() = "NodeJs"
}

@Suppress("RedundantSuspendModifier")
internal actual suspend fun <T> useLinesOfTextFile(filename: String, block: (Sequence<String>) -> T): T {
    val fh = fopen(filename, "r") ?: error("unable to open $filename")
    val buf = nativeHeap.allocArray<ByteVar>(512)
    return try {
        block(generateSequence {
            fgets(buf, 512, fh)?.let {
                buf.toKString().trimEnd()
            }
        })
    } finally {
        nativeHeap.free(buf)
        fclose(fh);
    }
}

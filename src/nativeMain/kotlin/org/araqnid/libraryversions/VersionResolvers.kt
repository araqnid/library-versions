package org.araqnid.libraryversions

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.readBytes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread

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
internal actual suspend fun readTextFile(filename: String): String {
    val fh = fopen(filename, "r") ?: error("unable to open $filename")
    return try {
        memScoped {
            val buf = allocArray<ByteVar>(2048)
            val parts = mutableListOf<String>()
            while (true) {
                val got = fread(buf, 1U, 2048U, fh)
                if (got == 0UL) break
                parts += buf.toKString(got.toInt())
            }
            when (parts.size) {
                0 -> ""
                1 -> parts[0]
                else -> parts.joinToString(separator = "")
            }
        }
    } finally {
        fclose(fh);
    }
}

private fun CPointer<ByteVar>.toKString(length: Int): String {
    val bytes = this.readBytes(length)
    return bytes.decodeToString()
}

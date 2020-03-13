package org.araqnid.libraryversions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@JsModule("fs")
private external object FS {
    fun readFile(path: String, encoding: String, callback: (Throwable?, String?) -> Unit)
}

internal actual suspend fun readTextFile(filename: String): String {
    return suspendCancellableCoroutine { cont ->
        FS.readFile(filename, "utf-8") { err, data ->
            if (err == null)
                cont.resume(data!!)
            else
                cont.resumeWithException(err)
        }
    }
}

actual object ZuluResolver : Resolver {
    override fun findVersions(httpFetcher: HttpFetcher): Flow<String> {
        return flowOf()
    }
}

package org.araqnid.libraryversions

import com.google.common.base.Throwables
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking

@OptIn(FlowPreview::class)
fun main(args: Array<String>) {
    runBlocking {
        showVersionsOnConsole(loadVersionResolvers(if (args.isNotEmpty()) args[0] else null), JdkHttpFetcher())
    }
}

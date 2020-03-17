package org.araqnid.libraryversions

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import org.araqnid.libraryversions.js.axios.Axios

@Suppress("unused")
@JsName("findLatestVersions")
fun findLatestVersions(configFile: String? = null): Job {
    return GlobalScope.launch {
        showVersionsOnConsole(loadVersionResolvers(configFile), AxiosHttpFetcher(Axios))
    }
}

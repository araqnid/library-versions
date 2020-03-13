package org.araqnid.libraryversions

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import kotlinx.html.body
import kotlinx.html.dd
import kotlinx.html.dl
import kotlinx.html.dt
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.html
import kotlinx.html.stream.appendHTML
import kotlinx.html.title
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import kotlin.coroutines.CoroutineContext

class IndexServlet(appContext: CoroutineContext) : HttpServlet(), CoroutineScope by CoroutineScope(appContext) {
    private val httpFetcher = JdkHttpFetcher()
    private val resolvers = defaultVersionResolvers

    @OptIn(FlowPreview::class)
    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        val pathInfo = req.pathInfo
        if (pathInfo != null) {
            resp.sendError(404)
            return
        }

        respondAsynchronouslyOrShowError(req, resp, CoroutineName("IndexServlet")) {
            resp.contentType = "text/plain"

            val versions = resolvers.map { resolver ->
                        resolver.findVersions(httpFetcher)
                                .map { version -> resolver to version }
                                .catch { ex ->
                                    emit(resolver to "FAILED: $ex")
                                }
                    }
                    .asFlow()
                    .flattenMerge()
                    .toList()

            withContext(Dispatchers.IO) {
                resp.contentType = "text/html"
                resp.writer.use { pw ->
                    pw.println("<!DOCTYPE html>")
                    pw.appendHTML(xhtmlCompatible = true).apply {
                        html {
                            head {
                                title {
                                    +"Latest versions"
                                }
                            }
                            body {
                                h1 {
                                    +"Latest versions"
                                }

                                dl {
                                    versions.sortedBy { (resolver, _) -> resolver.toString() }
                                            .forEach { (resolver, version) ->
                                                dt {
                                                    +"$resolver"
                                                }
                                                dd {
                                                    +version
                                                }
                                            }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

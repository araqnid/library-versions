package org.araqnid.libraryversions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import javax.servlet.AsyncEvent
import javax.servlet.AsyncListener
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun CoroutineScope.respondAsynchronously(request: HttpServletRequest, context: CoroutineContext = EmptyCoroutineContext, handler: suspend CoroutineScope.() -> Unit) {
    val asyncContext = request.startAsync()
    val job = launch(context, block = handler)
    asyncContext.addListener(object : AsyncListener {
        override fun onComplete(event: AsyncEvent) {
            job.cancel()
        }

        override fun onStartAsync(event: AsyncEvent) {
        }

        override fun onTimeout(event: AsyncEvent) {
            job.cancel()
        }

        override fun onError(event: AsyncEvent) {
        }
    })
    job.invokeOnCompletion {
        asyncContext.complete()
    }
}

private val logger by lazy { LoggerFactory.getLogger("org.araqnid.libraryversions.AsyncHttp")!! }

fun CoroutineScope.respondAsynchronouslyOrShowError(request: HttpServletRequest, response: HttpServletResponse, context: CoroutineContext = EmptyCoroutineContext, handler: suspend CoroutineScope.() -> Unit) {
    return respondAsynchronously(request, context) {
        try {
            coroutineScope(handler)
        } catch (e: Exception) {
            if (!response.isCommitted) {
                logger.error(e.toString(), e)
                response.status = 500
                response.contentType = "text/plain"
                withContext(Dispatchers.IO) {
                    response.writer.use { pw ->
                        e.printStackTrace(pw)
                    }
                }
            }
            else {
                logger.error("$e (response already committed)", e)
            }
        }
    }
}

package org.araqnid.libraryversions.server

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import org.eclipse.jetty.server.CustomRequestLog
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.Slf4jRequestLogWriter
import org.eclipse.jetty.server.handler.gzip.GzipHandler
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.util.thread.QueuedThreadPool
import kotlin.coroutines.CoroutineContext

fun main() {
    val port by env { it.toInt() }

    val app = App(port)
    Runtime.getRuntime().addShutdownHook(Thread(Runnable {
        app.stop()
    }, "shutdown"))
    app.start()
}

class App(httpPort: Int) : CoroutineScope {
    val threadPool = QueuedThreadPool().apply {
        name = "Jetty"
    }

    override val coroutineContext: CoroutineContext = threadPool.asCoroutineDispatcher() + SupervisorJob()

    val server = Server(threadPool).apply {
        addConnector(ServerConnector(this, 1, 1).apply {
            this.port = httpPort
        })
        handler = ServletContextHandler().apply {
            gzipHandler = GzipHandler()
            addServlet(ServletHolder(IndexServlet(coroutineContext)), "/")
        }
        requestLog = CustomRequestLog(Slf4jRequestLogWriter(), CustomRequestLog.NCSA_FORMAT)
    }

    fun start() {
        server.start()
    }

    fun stop() {
        cancel()
        server.stop()
    }
}

package org.araqnid.libraryversions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import java.net.http.HttpResponse
import java.nio.ByteBuffer
import java.util.concurrent.CompletionStage
import java.util.concurrent.Flow as JdkFlow

val flowBodyHandler: HttpResponse.BodyHandler<Flow<ByteBuffer>> =
    HttpResponse.BodyHandlers.ofPublisher()
        .adapt { publisher -> publisher.asFlow().flatMapConcat { buffers -> buffers.asFlow() } }

private fun <T, U> HttpResponse.BodyHandler<T>.adapt(fn: (T) -> U): HttpResponse.BodyHandler<U> =
    HttpResponse.BodyHandler<U> { responseInfo -> apply(responseInfo).adapt(fn) }

private fun <T, U> HttpResponse.BodySubscriber<T>.adapt(fn: (T) -> U): HttpResponse.BodySubscriber<U> {
    return object : HttpResponse.BodySubscriber<U> {
        override fun getBody(): CompletionStage<U> {
            return this@adapt.body.thenApply { body -> fn(body) }
        }

        override fun onSubscribe(subscription: JdkFlow.Subscription) {
            this@adapt.onSubscribe(subscription)
        }

        override fun onNext(item: List<ByteBuffer>) {
            this@adapt.onNext(item)
        }

        override fun onComplete() {
            this@adapt.onComplete()
        }

        override fun onError(throwable: Throwable) {
            this@adapt.onError(throwable)
        }
    }
}

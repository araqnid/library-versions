package org.araqnid.libraryversions

import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.receiveOrNull
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.internal.ChannelFlow
import kotlinx.coroutines.flow.internal.SendingCollector
import kotlinx.coroutines.plus
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.coroutineContext
import java.util.concurrent.Flow as JdkFlow

// All shamelessly cribbed from ReactiveFlow.kt in kotlinx-coroutines-reactive
// Since JDK Flow is supposed to be compatible with reactive streams, it makes sense

/**
 * Transforms the given JDK [Publisher][JdkFlow.Publisher] into [Flow].
 * Use [buffer] operator on the resulting flow to specify the size of the backpressure.
 * More precisely, it specifies the value of the subscription's [request][JdkFlow.Subscription.request].
 * [buffer] default capacity is used by default.
 *
 * If any of the resulting flow transformations fails, subscription is immediately cancelled and all in-flight elements
 * are discarded.
 */
fun <T : Any> JdkFlow.Publisher<T>.asFlow(): Flow<T> = JdkPublisherAsFlow(
        this)

@OptIn(InternalCoroutinesApi::class)
private class JdkPublisherAsFlow<T : Any>(
        private val publisher: JdkFlow.Publisher<T>,
        context: CoroutineContext = EmptyCoroutineContext,
        capacity: Int = Channel.BUFFERED
) : ChannelFlow<T>(context, capacity) {
    override fun create(context: CoroutineContext, capacity: Int): ChannelFlow<T> =
            JdkPublisherAsFlow(publisher, context, capacity)

    private val requestSize: Long
        get() = when (capacity) {
            Channel.CONFLATED -> Long.MAX_VALUE // request all and conflate incoming
            Channel.RENDEZVOUS -> 1L // need to request at least one anyway
            Channel.UNLIMITED -> Long.MAX_VALUE // reactive streams way to say "give all" must be Long.MAX_VALUE
            Channel.BUFFERED -> 64L // Channel.CHANNEL_DEFAULT_CAPACITY
            else -> capacity.toLong().also { check(it >= 1) }
        }

    override suspend fun collect(collector: FlowCollector<T>) {
        val collectContext = coroutineContext
        val newDispatcher = context[ContinuationInterceptor]
        if (newDispatcher == null || newDispatcher == collectContext[ContinuationInterceptor]) {
            // fast path -- subscribe directly in this dispatcher
            return collectImpl(collector)
        }
        // slow path -- produce in a separate dispatcher
        collectSlowPath(collector)
    }


    private suspend fun collectSlowPath(collector: FlowCollector<T>) {
        coroutineScope {
            collector.emitAll(produceImpl(this + context))
        }
    }

    private suspend fun collectImpl(collector: FlowCollector<T>) {
        val subscriber = JdkFlowSubscriber<T>(capacity, requestSize)
        publisher.subscribe(subscriber)
        try {
            var consumed = 0L
            while (true) {
                val value = subscriber.takeNextOrNull() ?: break
                collector.emit(value)
                if (++consumed == requestSize) {
                    consumed = 0L
                    subscriber.makeRequest()
                }
            }
        } finally {
            subscriber.cancel()
        }
    }

    // The second channel here is used for produceIn/broadcastIn and slow-path (dispatcher change)
    override suspend fun collectTo(scope: ProducerScope<T>) =
            collectImpl(SendingCollector(scope.channel))
}

private class JdkFlowSubscriber<T : Any> (
        capacity: Int,
        private val requestSize: Long
) : JdkFlow.Subscriber<T> {
    private lateinit var subscription: JdkFlow.Subscription
    private val channel = Channel<T>(capacity)

    suspend fun takeNextOrNull(): T? = channel.receiveOrNull()

    override fun onNext(item: T) {
        // Controlled by requestSize
        require(channel.offer(item)) { "Element $item was not added to channel because it was full, $channel" }
    }

    override fun onComplete() {
        channel.close()
    }

    override fun onError(throwable: Throwable?) {
        channel.close(throwable)
    }

    override fun onSubscribe(subscription: JdkFlow.Subscription) {
        this.subscription = subscription
        makeRequest()
    }

    fun makeRequest() {
        subscription.request(requestSize)
    }

    fun cancel() {
        subscription.cancel()
    }
}

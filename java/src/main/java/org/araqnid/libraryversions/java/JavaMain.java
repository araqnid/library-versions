package org.araqnid.libraryversions.java;

import kotlin.Result;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlin.coroutines.intrinsics.IntrinsicsKt;
import kotlinx.coroutines.CoroutineName;
import kotlinx.coroutines.CoroutineStart;
import kotlinx.coroutines.DelayKt;
import kotlinx.coroutines.GlobalScope;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.FlowKt;
import kotlinx.coroutines.future.FutureKt;
import org.araqnid.libraryversions.GradleResolver;
import org.araqnid.libraryversions.MavenResolverKt;
import org.araqnid.libraryversions.ZuluResolver;
import org.jetbrains.annotations.NotNull;

import java.net.http.HttpClient;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public class JavaMain {
    private static <T> CompletableFuture<Unit> collectFlowEasyWay(Flow<? extends T> flow, CoroutineContext context, Consumer<? super T> consumer) throws InterruptedException {
        return callSuspendFunction(context, cont -> (
                flow.collect((t, ignored) -> {
                    consumer.accept(t);
                    return Unit.INSTANCE;
                }, cont)
        ));
    }

    public static <T> CompletableFuture<T> callSuspendFunction(CoroutineContext context, Function<? super Continuation<? super T>, ?> call) {
        return FutureKt.future(GlobalScope.INSTANCE, context, CoroutineStart.DEFAULT, (scope, cont) -> call.apply(cont));
    }

    private static <T> CompletableFuture<Unit> collectFlowHardWay(Flow<? extends T> flow, CoroutineContext context, Consumer<? super T> consumer) throws InterruptedException {
        return callSuspendFunctionHardWay(context, cont -> (
                flow.collect((t, ignored) -> {
                    consumer.accept(t);
                    return Unit.INSTANCE;
                }, cont)
        ));
    }

    @SuppressWarnings("unchecked")
    public static <T> CompletableFuture<T> callSuspendFunctionHardWay(CoroutineContext context, Function<? super Continuation<? super T>, ?> call) {
        CompletableFuture<T> future = new CompletableFuture<>();

        try {
            Object result = call.apply(new Continuation<>() {
                @NotNull
                @Override
                public CoroutineContext getContext() {
                    return context;
                }

                @SuppressWarnings("KotlinInternalInJava")
                @Override
                public void resumeWith(@NotNull Object o) {
                    if (o instanceof Result.Failure) {
                        future.completeExceptionally(((Result.Failure) o).exception);
                    } else {
                        future.complete((T) o);
                    }
                }
            });
            if (result != IntrinsicsKt.getCOROUTINE_SUSPENDED())
                future.complete((T) result);
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }

        return future;
    }

    public static void main(String[] args) {
        HttpClient httpClient = HttpClient.newHttpClient();

        CompletableFuture<Unit> delayFuture = callSuspendFunction(EmptyCoroutineContext.INSTANCE, cont -> DelayKt.delay(1000L, cont));
        System.out.println(Instant.now() + " - starting delay");
        delayFuture.join();
        System.out.println(Instant.now() + " - finished delay");

        List<Flow<String>> flows = List.of(
                FlowKt.flowOf("red", "blue", "orange"),
                ZuluResolver.INSTANCE.findVersions(httpClient),
                GradleResolver.INSTANCE.findVersions(httpClient),
                MavenResolverKt.mavenCentral("org.jetbrains.kotlin", "kotlin-stdlib").findVersions(httpClient)
        );
        Flow<String> aggregateFlow = FlowKt.flattenMerge(FlowKt.asFlow(flows), 4);
        for (Flow<?> flow : List.of(aggregateFlow)) {
            try {
                collectFlowEasyWay(flow, new CoroutineName("EasyWay"), item -> {
                    System.out.printf("Produced: %s%n", item);
                }).join();
                System.out.println("Flow collected easy way");
                collectFlowHardWay(flow, new CoroutineName("HardWay"), item -> {
                    System.out.printf("Produced: %s%n", item);
                }).join();
                System.out.println("Flow collected hard way");
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
        System.out.println("All done");
    }
}

package com.alibaba.cloud.ai.graph.action;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;


public interface AsyncCommandAction extends BiFunction<OverAllState, RunnableConfig, CompletableFuture<Command>> {

    static AsyncCommandAction node_async(CommandAction syncAction) {
        return (state, config ) -> {
            var result = new CompletableFuture<Command>();
            try {
                result.complete(syncAction.apply(state, config));
            } catch (Exception e) {
                result.completeExceptionally(e);
            }
            return result;
        };
    }

    static  AsyncCommandAction of(AsyncEdgeAction action) {
        return (state, config) ->
                action.apply(state).thenApply(Command::new);
    }

}


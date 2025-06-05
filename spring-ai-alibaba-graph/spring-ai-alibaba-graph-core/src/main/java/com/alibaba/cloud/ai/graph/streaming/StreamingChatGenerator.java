package com.alibaba.cloud.ai.graph.streaming;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import org.bsc.async.AsyncGenerator;
import org.bsc.async.FlowGenerator;
import org.reactivestreams.FlowAdapters;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Optional.ofNullable;

public interface StreamingChatGenerator {

    class Builder {
        private Function<ChatResponse, Map<String, Object>> mapResult;
        private String startingNode;
        private OverAllState startingState;

        /**
         * Sets the mapping function for the builder.
         *
         * @param mapResult a function to map the response to a result
         * @return the builder instance
         */
        public Builder mapResult(Function<ChatResponse, Map<String, Object>> mapResult) {
            this.mapResult = mapResult;
            return this;
        }

        /**
         * Sets the starting node for the builder.
         *
         * @param node the starting node
         * @return the builder instance
         */
        public Builder startingNode(String node) {
            this.startingNode = node;
            return this;
        }

        /**
         * Sets the starting state for the builder.
         *
         * @param state the initial state
         * @return the builder instance
         */
        public Builder startingState(OverAllState state) {
            this.startingState = state;
            return this;
        }

        /**
         * Builds and returns an instance of LLMStreamingGenerator.
         *
         * @return a new instance of LLMStreamingGenerator
         */
        public AsyncGenerator<? extends NodeOutput> build(Flux<ChatResponse> flux) {
            Objects.requireNonNull(flux, "flux cannot be null");
            Objects.requireNonNull(mapResult, "mapResult cannot be null");

            var result = new AtomicReference<ChatResponse>(null);

            Consumer<ChatResponse> mergeMessage = (response) -> {
                result.updateAndGet(lastResponse -> {

                    if (lastResponse == null) {
                        return response;
                    }

                    var currentMessage = response.getResult().getOutput();

                    if (currentMessage.hasToolCalls()) {
                        return response;
                    }

                    var lastMessage = lastResponse.getResult().getOutput();

                    var newMessage = new AssistantMessage(
                            ofNullable(currentMessage.getText())
                                    .map(text -> lastMessage.getText().concat(text))
                                    .orElse(lastMessage.getText()),
                            currentMessage.getMetadata(),
                            currentMessage.getToolCalls(),
                            currentMessage.getMedia()
                    );

                    var newGeneration = new Generation(newMessage, response.getResult().getMetadata());
                    return new ChatResponse(List.of(newGeneration), response.getMetadata());

                });
            };

            var processedFlux = flux
                    .doOnNext(next -> mergeMessage.accept(next))
                    .map(next ->
                            new StreamingOutput(next.getResult().getOutput().getText(),
                                    startingNode,
                                    startingState)
                    );

            return FlowGenerator.fromPublisher(
                    FlowAdapters.toFlowPublisher(processedFlux),
                    () -> mapResult.apply(result.get()));
        }
    }

    static Builder builder() {
        return new Builder();
    }
}

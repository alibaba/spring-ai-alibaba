/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph.streaming;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.async.AsyncGenerator;
import com.alibaba.cloud.ai.graph.async.FlowGenerator;
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

/**
 * A generator interface for streaming chat responses in a reactive manner. It provides a
 * fluent API to configure and build a streaming generator that processes chat responses
 * and produces output based on the streamed data.
 */
public interface StreamingChatGenerator {

	/**
	 * Builder class for creating instances of {@link AsyncGenerator} that process chat
	 * responses. This builder allows setting mapping logic, starting node, and initial
	 * state before building.
	 */
	class Builder {

		private Function<ChatResponse, Map<String, Object>> mapResult;

		private String startingNode;

		private OverAllState startingState;

		/**
		 * Sets the mapping function that converts a ChatResponse into a Map result.
		 * @param mapResult a function to transform the final chat response into a result
		 * map
		 * @return the builder instance for method chaining
		 */
		public Builder mapResult(Function<ChatResponse, Map<String, Object>> mapResult) {
			this.mapResult = mapResult;
			return this;
		}

		/**
		 * Sets the starting node for the streaming process.
		 * @param node the identifier of the starting node in the flow
		 * @return the builder instance for method chaining
		 */
		public Builder startingNode(String node) {
			this.startingNode = node;
			return this;
		}

		/**
		 * Sets the initial state for the streaming process.
		 * @param state the overall state to start with
		 * @return the builder instance for method chaining
		 */
		public Builder startingState(OverAllState state) {
			this.startingState = state;
			return this;
		}

		/**
		 * Builds and returns an instance of AsyncGenerator that processes chat responses.
		 * The generator merges partial responses and maps them to final output.
		 * @param flux a Flux stream of ChatResponse objects
		 * @return an AsyncGenerator that produces NodeOutput instances
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
							Objects.requireNonNull(ofNullable(currentMessage.getText()).map(text -> {
								assert lastMessage.getText() != null;
								return lastMessage.getText().concat(text);
							}).orElse(lastMessage.getText())), currentMessage.getMetadata(),
							currentMessage.getToolCalls(), currentMessage.getMedia());

					var newGeneration = new Generation(newMessage, response.getResult().getMetadata());
					return new ChatResponse(List.of(newGeneration), response.getMetadata());

				});
			};

			var processedFlux = flux.doOnNext(mergeMessage::accept)
				.map(next -> new StreamingOutput(next.getResult().getOutput().getText(), startingNode, startingState));

			return FlowGenerator.fromPublisher(FlowAdapters.toFlowPublisher(processedFlux),
					() -> mapResult.apply(result.get()));
		}

	}

	/**
	 * Returns a new instance of the StreamingChatGenerator builder.
	 * @return a new builder instance
	 */
	static Builder builder() {
		return new Builder();
	}

}

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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import com.alibaba.cloud.ai.graph.GraphResponse;
import com.alibaba.cloud.ai.graph.OverAllState;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import static java.util.Objects.requireNonNull;

public interface FluxConverter {

	/**
	 * Builder class for creating instances of {@link Flux} that process chat responses.
	 * This builder allows setting mapping logic, starting node, and initial state before
	 * building.
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

		public Flux<GraphResponse<StreamingOutput>> build(Flux<ChatResponse> flux) {
			return buildInternal(flux,
					chatResponse -> new StreamingOutput(chatResponse.getResult().getOutput().getText(), startingNode,
							startingState));
		}

		private Flux<GraphResponse<StreamingOutput>> buildInternal(Flux<ChatResponse> flux,
				Function<ChatResponse, StreamingOutput> outputMapper) {
			Objects.requireNonNull(flux, "flux cannot be null");
			Objects.requireNonNull(mapResult, "mapResult cannot be null");

			var result = new AtomicReference<ChatResponse>(null);

			Consumer<ChatResponse> mergeMessage = (response) -> {
				result.updateAndGet(lastResponse -> {

					if (lastResponse == null) {
						return response;
					}

					final var currentMessage = response.getResult().getOutput();

					if (currentMessage.hasToolCalls()) {
						return response;
					}

					final var lastMessageText = requireNonNull(lastResponse.getResult().getOutput().getText(),
							"lastResponse text cannot be null");

					final var currentMessageText = currentMessage.getText();

					var newMessage = new AssistantMessage(
							currentMessageText != null ? lastMessageText.concat(currentMessageText) : lastMessageText,
							currentMessage.getMetadata(), currentMessage.getToolCalls(), currentMessage.getMedia());

					var newGeneration = new Generation(newMessage, response.getResult().getMetadata());
					return new ChatResponse(List.of(newGeneration), response.getMetadata());

				});
			};

			return flux.filter(response -> response.getResult() != null && response.getResult().getOutput() != null)
				.doOnNext(mergeMessage)
				.map(next -> GraphResponse
					.of(new StreamingOutput(next.getResult().getOutput().getText(), startingNode, startingState)))
				.concatWith(Mono.fromCallable(() -> {
					Map<String, Object> completionResult = mapResult.apply(result.get());
					return GraphResponse.done(completionResult);
				}));
		}

	}

	static Builder builder() {
		return new Builder();
	}

}

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

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public interface GraphFluxGenerator {

	/**
	 * Builder class for creating instances of {@link Flux} that process chat responses.
	 * This builder allows setting mapping logic, starting node, and initial state before
	 * building.
	 */
	class Builder {


		private String startingNode;

		private String outKey;



		/**
		 * Sets the starting node for the streaming process.
		 * @param node the identifier of the starting node in the flow
		 * @return the builder instance for method chaining
		 */
		public Builder startingNode(String node) {
			this.startingNode = node;
			return this;
		}


		public Builder outKey(String outKey) {
			this.outKey = outKey;
			return this;
		}


		public GraphFlux<ChatResponse> build(Flux<ChatResponse> flux) {
			return buildInternal(flux);
		}


			private GraphFlux<ChatResponse> buildInternal(Flux<ChatResponse> flux) {
				Objects.requireNonNull(flux, "flux cannot be null");

				var result = new AtomicReference<ChatResponse>(null);

				Function<ChatResponse, ChatResponse> mergeMessage = (response) -> result.updateAndGet(lastResponse -> {

					AssistantMessage currentMessage = extractAssistantMessage(response);
					if (currentMessage == null) {
						// Usage-only chunk or no AssistantMessage – keep previous aggregated response
						return lastResponse;
					}

					if (lastResponse == null) {
						// Normalize so that getResult().getOutput() reflects the selected message
						var generation = new Generation(currentMessage,
								response.getResult() != null ? response.getResult().getMetadata() : null);
						return new ChatResponse(List.of(generation), response.getMetadata());
					}

					AssistantMessage lastMessage = extractAssistantMessage(lastResponse);

					if (currentMessage.hasToolCalls()) {
						// New tool-call message overrides previous aggregated content
						var generation = new Generation(currentMessage,
								response.getResult() != null ? response.getResult().getMetadata() : null);
						return new ChatResponse(List.of(generation), response.getMetadata());
					}

					final var lastMessageText = requireNonNull(
							lastMessage != null ? lastMessage.getText() : null,
							"lastResponse text cannot be null");

					final var currentMessageText = currentMessage.getText();

					var newMessage = AssistantMessage.builder()
							.content(currentMessageText != null
									? lastMessageText.concat(currentMessageText)
									: lastMessageText)
							.properties(currentMessage.getMetadata())
							.toolCalls(currentMessage.getToolCalls())
							.media(currentMessage.getMedia())
							.build();

					var newGeneration = new Generation(newMessage,
							response.getResult() != null ? response.getResult().getMetadata() : null);
					return new ChatResponse(List.of(newGeneration), response.getMetadata());

				});

				return GraphFlux.of(startingNode, outKey, flux, mergeMessage, response -> {
					AssistantMessage message = extractAssistantMessage(response);
					return message != null ? message.getText() : null;
				});
			}

			/**
			 * Extracts the most appropriate AssistantMessage from a ChatResponse for streaming.
			 * <p>
			 * Prefers AssistantMessage generations that contain tool calls, then falls back to
			 * the last non-null AssistantMessage. Returns {@code null} when no suitable
			 * AssistantMessage exists (e.g. usage-only chunks).
			 */
			private AssistantMessage extractAssistantMessage(ChatResponse response) {
				if (response == null) {
					return null;
				}

				try {
					List<Generation> generations = response.getResults();
					if (generations != null && !generations.isEmpty()) {
						AssistantMessage fallback = null;
						for (Generation generation : generations) {
							if (generation == null) {
								continue;
							}
							var output = generation.getOutput();
							if (output instanceof AssistantMessage assistantMessage) {
								if (assistantMessage.hasToolCalls()) {
									// Prefer the first message that contains tool calls
									return assistantMessage;
								}
								// Remember the last non-null assistant message as a fallback
								fallback = assistantMessage;
							}
						}
						if (fallback != null) {
							return fallback;
						}
					}
				}
				catch (Exception ex) {
					// Defensive: if the underlying implementation changes and getResults() fails,
					// fall back to getResult().
				}

				if (response.getResult() != null
						&& response.getResult().getOutput() instanceof AssistantMessage assistant) {
					return assistant;
				}

				return null;
			}

		}

	static Builder builder() {
		return new Builder();
	}

}

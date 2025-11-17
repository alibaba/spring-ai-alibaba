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

			Function<ChatResponse,ChatResponse> mergeMessage = (response) -> result.updateAndGet(lastResponse -> {

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

			return GraphFlux.of(startingNode, outKey, flux, mergeMessage, response -> response.getResult().getOutput().getText());
		}

	}

	static Builder builder() {
		return new Builder();
	}

}

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

import com.alibaba.cloud.ai.graph.util.AssistantMessageUtils;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Factory for building {@link GraphFlux} instances from streaming {@link ChatResponse}
 * objects. Handles aggregation of streaming chunks, including:
 * <ul>
 *     <li>Merging assistant text across chunks</li>
 *     <li>Preserving and merging tool calls across chunks</li>
 *     <li>Normalizing aggregated responses so {@link ChatResponse#getResult()} always
 *     reflects the merged {@link AssistantMessage}</li>
 * </ul>
 */
public interface GraphFluxGenerator {

	/**
	 * Builder class for creating instances of {@link GraphFlux} that process chat
	 * responses.
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

		/**
		 * Sets the storage key under which the final aggregated result will be stored.
		 * @param outKey the storage key
		 * @return the builder instance for method chaining
		 */
		public Builder outKey(String outKey) {
			this.outKey = outKey;
			return this;
		}

		/**
		 * Build a {@link GraphFlux} from a streaming {@link Flux} of {@link ChatResponse}
		 * objects.
		 * @param flux the streaming flux
		 * @return GraphFlux wrapping the streaming responses
		 */
		public GraphFlux<ChatResponse> build(Flux<ChatResponse> flux) {
			return buildInternal(flux);
		}

		private GraphFlux<ChatResponse> buildInternal(Flux<ChatResponse> flux) {
			Objects.requireNonNull(flux, "flux cannot be null");

			// Holds the aggregated ChatResponse across streaming chunks
			AtomicReference<ChatResponse> aggregatedRef = new AtomicReference<>(null);

			Function<ChatResponse, ChatResponse> mergeMessage = response -> aggregatedRef.updateAndGet(lastResponse -> {

				AssistantMessage currentMessage = AssistantMessageUtils.extractAssistantMessage(response);
				if (currentMessage == null) {
					// Usage-only chunk or no AssistantMessage – keep previous aggregated response
					return lastResponse;
				}

				AssistantMessage mergedMessage;
				if (lastResponse == null) {
					// First valid AssistantMessage – start aggregation from here.
					mergedMessage = currentMessage;
				}
				else {
					AssistantMessage lastMessage = AssistantMessageUtils.extractAssistantMessage(lastResponse);

					// Append current text to previous text, tolerating null text.
					String lastText = lastMessage != null ? lastMessage.getText() : null;
					String currentText = currentMessage.getText();
					String mergedText = (lastText != null ? lastText : "")
							+ (currentText != null ? currentText : "");

					List<AssistantMessage.ToolCall> mergedToolCalls = AssistantMessageUtils.mergeToolCalls(
							lastMessage != null ? lastMessage.getToolCalls() : List.of(),
							currentMessage.getToolCalls());

					mergedMessage = AssistantMessage.builder()
							.content(mergedText.isEmpty() ? null : mergedText)
							.properties(currentMessage.getMetadata())
							.toolCalls(mergedToolCalls)
							.media(currentMessage.getMedia())
							.build();
				}

				// Normalize so that getResult().getOutput() reflects the aggregated message
				Generation generation = new Generation(mergedMessage,
						response.getResult() != null ? response.getResult().getMetadata() : null);
				return new ChatResponse(List.of(generation), response.getMetadata());
			});

			return GraphFlux.of(
					startingNode,
					outKey,
					flux,
					mergeMessage,
					// Chunk result: per-chunk text view (for streaming output)
					response -> {
						AssistantMessage message = AssistantMessageUtils.extractAssistantMessage(response);
						return message != null ? message.getText() : null;
					});
		}
	}

	static Builder builder() {
		return new Builder();
	}

}


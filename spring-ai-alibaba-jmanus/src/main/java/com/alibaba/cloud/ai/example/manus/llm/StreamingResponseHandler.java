/*
 * Copyright 2025 the original author or authors.
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
package com.alibaba.cloud.ai.example.manus.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A utility class for handling streaming chat responses with periodic progress logging.
 * This class merges text content and tool calls from multiple streaming responses and
 * provides regular progress updates to prevent users from thinking the model is
 * unresponsive.
 */
@Component
public class StreamingResponseHandler {

	private static final Logger log = LoggerFactory.getLogger(StreamingResponseHandler.class);

	/**
	 * Result container for streaming response processing
	 */
	public static class StreamingResult {

		private final String mergedText;

		private final List<ToolCall> mergedToolCalls;

		private final ChatResponse lastResponse;

		public StreamingResult(String mergedText, List<ToolCall> mergedToolCalls, ChatResponse lastResponse) {
			this.mergedText = mergedText;
			this.mergedToolCalls = mergedToolCalls;
			this.lastResponse = lastResponse;
		}

		public String getMergedText() {
			return mergedText;
		}

		public List<ToolCall> getMergedToolCalls() {
			return mergedToolCalls;
		}

		public ChatResponse getLastResponse() {
			return lastResponse;
		}

		/**
		 * Get tool calls, preferring merged calls if available, otherwise fall back to
		 * last response
		 */
		public List<ToolCall> getEffectiveToolCalls() {
			return mergedToolCalls.isEmpty() && lastResponse != null && lastResponse.getResult() != null
					&& lastResponse.getResult().getOutput() != null
							? lastResponse.getResult().getOutput().getToolCalls() : mergedToolCalls;
		}

		/**
		 * Get text content, preferring merged text if available, otherwise fall back to
		 * last response
		 */
		public String getEffectiveText() {
			return mergedText.isEmpty() && lastResponse != null && lastResponse.getResult() != null
					&& lastResponse.getResult().getOutput() != null ? lastResponse.getResult().getOutput().getText()
							: mergedText;
		}

	}

	/**
	 * Process a streaming chat response flux with periodic progress logging
	 * @param responseFlux The streaming chat response flux
	 * @param contextName A descriptive name for logging context (e.g., "Agent thinking",
	 * "Plan creation")
	 * @return StreamingResult containing merged content and the last response
	 */
	public StreamingResult processStreamingResponse(Flux<ChatResponse> responseFlux, String contextName) {
		StringBuilder responseTextBuilder = new StringBuilder();
		List<ToolCall> allToolCalls = new ArrayList<>();
		AtomicInteger responseCount = new AtomicInteger(0);
		AtomicReference<String> currentText = new AtomicReference<>("");
		AtomicReference<Integer> currentToolCallCount = new AtomicReference<>(0);

		AtomicReference<Long> lastLogTime = new AtomicReference<>(System.currentTimeMillis());

		ChatResponse lastResponse = responseFlux.doOnNext(chatResponse -> {
			responseCount.incrementAndGet();

			if (chatResponse.getResult() != null && chatResponse.getResult().getOutput() != null) {
				// Merge text content
				if (chatResponse.getResult().getOutput().getText() != null) {
					responseTextBuilder.append(chatResponse.getResult().getOutput().getText());
					currentText.set(responseTextBuilder.toString());
				}

				// Merge tool calls
				if (chatResponse.getResult().getOutput().getToolCalls() != null
						&& !chatResponse.getResult().getOutput().getToolCalls().isEmpty()) {
					allToolCalls.addAll(chatResponse.getResult().getOutput().getToolCalls());
					currentToolCallCount.set(allToolCalls.size());
				}
			}

			// Check if 10 seconds have passed since last log output
			long currentTime = System.currentTimeMillis();
			long timeSinceLastLog = currentTime - lastLogTime.get();
			if (timeSinceLastLog >= 10000) { // 10 seconds = 10000 milliseconds
				logProgress(contextName, currentText.get(), currentToolCallCount.get(), responseCount.get());
				lastLogTime.set(currentTime);
			}
		}).doOnComplete(() -> {
			// Log final result when streaming completes
			logCompletion(contextName, currentText.get(), currentToolCallCount.get(), responseCount.get());
		}).doOnError(error -> {
			log.error("Error during streaming response processing for {}: {}", contextName, error.getMessage());
		}).blockLast();

		return new StreamingResult(responseTextBuilder.toString(), allToolCalls, lastResponse);
	}

	/**
	 * Process a streaming chat response flux for text-only content (e.g., summaries)
	 * @param responseFlux The streaming chat response flux
	 * @param contextName A descriptive name for logging context
	 * @return The merged text content
	 */
	public String processStreamingTextResponse(Flux<ChatResponse> responseFlux, String contextName) {
		StreamingResult result = processStreamingResponse(responseFlux, contextName);
		return result.getEffectiveText();
	}

	private void logProgress(String contextName, String currentText, int toolCallCount, int responseCount) {
		int textLength = currentText != null ? currentText.length() : 0;
		String preview = getTextPreview(currentText, 100); // Show first 100 chars

		log.info("🔄 {} - Progress: {} responses received, {} characters, {} tool calls. Preview: '{}'", contextName,
				responseCount, textLength, toolCallCount, preview);
	}

	private void logCompletion(String contextName, String finalText, int toolCallCount, int responseCount) {
		int textLength = finalText != null ? finalText.length() : 0;
		String preview = getTextPreview(finalText, 200); // Show first 200 chars for
															// completion

		log.info("✅ {} - Completed: {} responses processed, {} characters, {} tool calls. Preview: '{}'", contextName,
				responseCount, textLength, toolCallCount, preview);
	}

	private String getTextPreview(String text, int maxLength) {
		if (text == null || text.isEmpty()) {
			return "(empty)";
		}
		if (text.length() <= maxLength) {
			return text;
		}
		return text.substring(0, maxLength) + "...";
	}

}

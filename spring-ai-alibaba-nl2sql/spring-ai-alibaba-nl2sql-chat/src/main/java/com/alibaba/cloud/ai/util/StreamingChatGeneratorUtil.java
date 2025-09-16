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

package com.alibaba.cloud.ai.util;

import com.alibaba.cloud.ai.enums.StreamResponseType;
import com.alibaba.cloud.ai.graph.GraphResponse;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.streaming.FluxConverter;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.function.Function;

/**
 * StreamingChatGenerator utility class for creating and configuring
 * StreamingChatGenerator instances
 *
 * @author zhangshenghang
 */
public class StreamingChatGeneratorUtil {

	private static final Logger logger = LoggerFactory.getLogger(StreamingChatGeneratorUtil.class);

	/**
	 * Create a StreamingChatGenerator with a custom result mapping function
	 * @param nodeName node name
	 * @param state state
	 * @param mapResultFunction result mapping function
	 * @param flux response stream
	 * @return AsyncGenerator instance
	 */
	public static Flux<GraphResponse<StreamingOutput>> createGenerator(String nodeName, OverAllState state,
			Function<ChatResponse, Map<String, Object>> mapResultFunction, Flux<ChatResponse> flux) {
		return FluxConverter.builder()
			.startingNode(nodeName)
			.startingState(state)
			.mapResult(mapResultFunction)
			.build(flux);
	}

	public static class StreamingProcessorBuilder {

		private String startMessage;

		private String completionMessage;

		private StreamResponseType type = StreamResponseType.EXPLANATION;

		private Function<ChatResponse, String> contentExtractor = response -> response.getResult()
			.getOutput()
			.getText();

		private String nodeName;

		private OverAllState state;

		private Function<String, Map<String, Object>> resultMapper;

		private Function<OverAllState, Map<String, Object>> businessLogicExecutor;

		private boolean trimResult = true;

		private StreamingProcessorBuilder() {
		}

		public StreamingProcessorBuilder startMessage(String startMessage) {
			this.startMessage = startMessage;
			return this;
		}

		public StreamingProcessorBuilder completionMessage(String completionMessage) {
			this.completionMessage = completionMessage;
			return this;
		}

		public StreamingProcessorBuilder type(StreamResponseType type) {
			this.type = type;
			return this;
		}

		public StreamingProcessorBuilder contentExtractor(Function<ChatResponse, String> contentExtractor) {
			this.contentExtractor = contentExtractor;
			return this;
		}

		/**
		 * Set node name
		 */
		public StreamingProcessorBuilder nodeName(String nodeName) {
			this.nodeName = nodeName;
			return this;
		}

		/**
		 * Set node class (automatically extract class name as node name)
		 */
		public StreamingProcessorBuilder nodeClass(Class<?> nodeClass) {
			this.nodeName = nodeClass.getSimpleName();
			return this;
		}

		/**
		 * Set state
		 */
		public StreamingProcessorBuilder state(OverAllState state) {
			this.state = state;
			return this;
		}

		/**
		 * Set result mapping function (convert collected content to final returned Map)
		 */
		public StreamingProcessorBuilder resultMapper(Function<String, Map<String, Object>> resultMapper) {
			this.resultMapper = resultMapper;
			return this;
		}

		/**
		 * Set whether to trim the final result
		 */
		public StreamingProcessorBuilder trimResult(boolean trimResult) {
			this.trimResult = trimResult;
			return this;
		}

		/**
		 * Set business logic executor (used to execute business logic in mapResult)
		 */
		public StreamingProcessorBuilder businessLogicExecutor(
				Function<OverAllState, Map<String, Object>> businessLogicExecutor) {
			this.businessLogicExecutor = businessLogicExecutor;
			return this;
		}

		/**
		 * Build streaming processor
		 * @param sourceFlux source data stream
		 * @return AsyncGenerator instance
		 */
		public Flux<GraphResponse<StreamingOutput>> build(Flux<ChatResponse> sourceFlux) {
			if (nodeName == null || state == null) {
				throw new IllegalArgumentException("Node name and state cannot be null");
			}

			// If there is a business logic executor, use it first
			if (businessLogicExecutor != null) {
				return createGenerator(nodeName, state, response -> businessLogicExecutor.apply(state), sourceFlux);
			}

			// Otherwise use the original result mapping logic
			if (resultMapper == null) {
				throw new IllegalArgumentException("Result mapper cannot be null");
			}

			// Used to collect actual processing results
			final StringBuilder collectedResult = new StringBuilder();

			// Create wrapped stream
			Flux<ChatResponse> wrappedFlux = Flux.create(emitter -> {
				try {
					// 1. Send start message
					if (startMessage != null && !startMessage.isEmpty()) {
						emitter.next(ChatResponseUtil.createCustomStatusResponse(startMessage, type));
					}

					// 2. Process source data stream
					sourceFlux.doOnNext(response -> {
						// Extract and collect actual content
						String content = contentExtractor.apply(response);
						// if(JSONValidator.from(content).validate()){
						// content = JSONObject.parseObject(content).getString("data");
						// }
						if (content != null) {
							collectedResult.append(content);
						}
						// Send response to stream for user viewing
						emitter.next(ChatResponseUtil.createStatusResponse(response.getResult().getOutput().getText(),
								type));
					}).doOnComplete(() -> {
						// 3. Send completion message
						if (completionMessage != null && !completionMessage.isEmpty()) {
							emitter.next(ChatResponseUtil.createCustomStatusResponse("\n" + completionMessage, type));
						}
						logger.debug("[{}] Streaming processing completed", nodeName);
						emitter.complete();
					}).doOnError(error -> {
						logger.error("[{}] Error in streaming processing", nodeName, error);
						emitter.error(error);
					}).subscribe();
				}
				catch (Exception e) {
					logger.error("[{}] Failed to start streaming processing", nodeName, e);
					emitter.error(e);
				}
			});

			// Create generator using collected result
			return createGenerator(nodeName, state, response -> {
				String finalResult = collectedResult.toString();
				if (trimResult) {
					finalResult = finalResult.trim();
				}
				return resultMapper.apply(finalResult);
			}, wrappedFlux);
		}

	}

	/**
	 * Create streaming processor builder instance
	 * @return StreamingProcessorBuilder instance
	 */
	public static StreamingProcessorBuilder createStreamingProcessor() {
		return new StreamingProcessorBuilder();
	}

	/**
	 * Quickly create streaming generator with start and end messages
	 * @param nodeClass node class
	 * @param state state
	 * @param startMessage start message
	 * @param completionMessage completion message
	 * @param resultMapper result mapping function
	 * @param sourceFlux source data stream
	 * @return AsyncGenerator instance
	 */
	public static Flux<GraphResponse<StreamingOutput>> createStreamingGeneratorWithMessages(Class<?> nodeClass,
			OverAllState state, String startMessage, String completionMessage,
			Function<String, Map<String, Object>> resultMapper, Flux<ChatResponse> sourceFlux) {

		return createStreamingProcessor().nodeClass(nodeClass)
			.state(state)
			.startMessage(startMessage)
			.completionMessage(completionMessage)
			.resultMapper(resultMapper)
			.build(sourceFlux);
	}

	public static Flux<GraphResponse<StreamingOutput>> createStreamingGeneratorWithMessages(Class<?> nodeClass,
			OverAllState state, String startMessage, String completionMessage,
			Function<String, Map<String, Object>> resultMapper, Flux<ChatResponse> sourceFlux,
			StreamResponseType type) {

		return createStreamingProcessor().nodeClass(nodeClass)
			.state(state)
			.startMessage(startMessage)
			.type(type)
			.completionMessage(completionMessage)
			.resultMapper(resultMapper)
			.build(sourceFlux);
	}

	public static Flux<GraphResponse<StreamingOutput>> createStreamingGeneratorWithMessages(Class<?> nodeClass,
			OverAllState state, Function<String, Map<String, Object>> resultMapper, Flux<ChatResponse> sourceFlux,
			StreamResponseType type) {

		return createStreamingProcessor().nodeClass(nodeClass)
			.state(state)
			.type(type)
			.resultMapper(resultMapper)
			.build(sourceFlux);
	}

	public static Flux<GraphResponse<StreamingOutput>> createStreamingGeneratorWithMessages(Class<?> nodeClass,
			OverAllState state, Function<String, Map<String, Object>> resultMapper, Flux<ChatResponse> sourceFlux) {

		return createStreamingProcessor().nodeClass(nodeClass)
			.state(state)
			.resultMapper(resultMapper)
			.build(sourceFlux);
	}

}

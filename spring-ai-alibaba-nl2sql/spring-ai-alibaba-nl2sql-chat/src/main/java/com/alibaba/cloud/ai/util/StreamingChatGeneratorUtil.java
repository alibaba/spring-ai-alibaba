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

import com.alibaba.cloud.ai.constant.StreamResponseType;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.async.AsyncGenerator;
import com.alibaba.cloud.ai.graph.streaming.StreamingChatGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
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
	 * Create a basic StreamingChatGenerator with an empty mapResult
	 * @param nodeName node name
	 * @param state state
	 * @param flux response stream
	 * @return AsyncGenerator instance
	 */
	public static AsyncGenerator<? extends NodeOutput> createEmptyGenerator(String nodeName, OverAllState state,
			Flux<ChatResponse> flux) {
		AsyncGenerator<? extends NodeOutput> build = StreamingChatGenerator.builder()
			.startingNode(nodeName)
			.startingState(state)
			.mapResult(chatResponse -> Map.of())
			.build(flux);
		return build;
	}

	/**
	 * Create a StreamingChatGenerator for streaming and printing text
	 * @param text text to print
	 * @return AsyncGenerator instance
	 */
	public static AsyncGenerator<? extends NodeOutput> createStreamPrintGenerator(String text) {
		AsyncGenerator<? extends NodeOutput> build = StreamingChatGenerator.builder()
			.mapResult(chatResponse -> Map.of())
			.build(Flux.just(ChatResponseUtil.createCustomStatusResponse(text)));
		return build;
	}

	/**
	 * Create an empty StreamingChatGenerator with an empty mapResult
	 * @param flux response stream
	 * @return AsyncGenerator instance
	 */
	public static AsyncGenerator<? extends NodeOutput> createEmptyGenerator(Flux<ChatResponse> flux) {
		AsyncGenerator<? extends NodeOutput> build = StreamingChatGenerator.builder()
			.mapResult(chatResponse -> Map.of())
			.build(flux);
		return build;
	}

	/**
	 * Create a StreamingChatGenerator with a custom result mapping function
	 * @param nodeName node name
	 * @param state state
	 * @param mapResultFunction result mapping function
	 * @param flux response stream
	 * @return AsyncGenerator instance
	 */
	public static AsyncGenerator<? extends NodeOutput> createGenerator(String nodeName, OverAllState state,
			Function<ChatResponse, Map<String, Object>> mapResultFunction, Flux<ChatResponse> flux) {
		AsyncGenerator<? extends NodeOutput> build = StreamingChatGenerator.builder()
			.startingNode(nodeName)
			.startingState(state)
			.mapResult(mapResultFunction)
			.build(flux);
		return build;
	}

	/**
	 * Create a StreamingChatGenerator using the class object to obtain the node name
	 * @param nodeClass node class
	 * @param state state
	 * @param flux response stream
	 * @return AsyncGenerator instance
	 */
	public static AsyncGenerator<? extends NodeOutput> createEmptyGenerator(Class<?> nodeClass, OverAllState state,
			Flux<ChatResponse> flux) {
		return createEmptyGenerator(nodeClass.getSimpleName(), state, flux);
	}

	/**
	 * Create a StreamingChatGenerator using the class object to obtain the node name and
	 * a custom result mapping function
	 * @param nodeClass node class
	 * @param state state
	 * @param mapResultFunction result mapping function
	 * @param flux response stream
	 * @return AsyncGenerator instance
	 */
	public static AsyncGenerator<? extends NodeOutput> createGenerator(Class<?> nodeClass, OverAllState state,
			Function<ChatResponse, Map<String, Object>> mapResultFunction, Flux<ChatResponse> flux) {
		return createGenerator(nodeClass.getSimpleName(), state, mapResultFunction, flux);
	}

	/**
	 * Create a StreamingChatGenerator with a completion callback When the generator
	 * finishes processing, the completion callback will be executed
	 * @param nodeClass node class
	 * @param state state
	 * @param mapResultFunction result mapping function
	 * @param flux response stream
	 * @param onCompleteCallback callback function executed upon completion, providing the
	 * final processing result as a parameter
	 * @return AsyncGenerator instance
	 */
	public static AsyncGenerator<? extends NodeOutput> createGeneratorWithCallback(Class<?> nodeClass,
			OverAllState state, Function<ChatResponse, Map<String, Object>> mapResultFunction, Flux<ChatResponse> flux,
			Consumer<Map<String, Object>> onCompleteCallback) {

		// Create a CompletableFuture to execute the callback when processing is complete
		CompletableFuture<Map<String, Object>> resultFuture = new CompletableFuture<>();

		// Add signal handling for the last element
		Flux<ChatResponse> wrappedFlux = flux.doOnNext(response -> {
			try {
				// Attempt to save the last result to the future
				Map<String, Object> result = mapResultFunction.apply(response);
				resultFuture.complete(result);
			}
			catch (Exception e) {
				logger.error("Error occurred while processing response", e);
			}
		}).doFinally(signalType -> {
			if (signalType == SignalType.ON_COMPLETE) {
				try {
					// If the future is already complete, get the result and execute the
					// callback
					if (resultFuture.isDone()) {
						onCompleteCallback.accept(resultFuture.get());
					}
				}
				catch (Exception e) {
					logger.error("Error occurred while executing completion callback", e);
				}
			}
		});

		// Create and return AsyncGenerator
		return StreamingChatGenerator.builder()
			.startingNode(nodeClass.getSimpleName())
			.startingState(state)
			.mapResult(mapResultFunction)
			.build(wrappedFlux);
	}

	/**
	 * Create a StreamingChatGenerator with a completion callback When the generator
	 * finishes processing, the completion callback will be executed
	 * @param nodeName node name
	 * @param state state
	 * @param mapResultFunction result mapping function
	 * @param flux response stream
	 * @param onCompleteCallback callback function executed upon completion, providing the
	 * final processing result as a parameter
	 * @return AsyncGenerator instance
	 */
	public static AsyncGenerator<? extends NodeOutput> createGeneratorWithCallback(String nodeName, OverAllState state,
			Function<ChatResponse, Map<String, Object>> mapResultFunction, Flux<ChatResponse> flux,
			Consumer<Map<String, Object>> onCompleteCallback) {

		// Similar to the method above, but uses the provided node name
		CompletableFuture<Map<String, Object>> resultFuture = new CompletableFuture<>();

		Flux<ChatResponse> wrappedFlux = flux.doOnNext(response -> {
			try {
				Map<String, Object> result = mapResultFunction.apply(response);
				resultFuture.complete(result);
			}
			catch (Exception e) {
				logger.error("Error occurred while processing response", e);
			}
		}).doFinally(signalType -> {
			if (signalType == SignalType.ON_COMPLETE) {
				try {
					if (resultFuture.isDone()) {
						onCompleteCallback.accept(resultFuture.get());
					}
				}
				catch (Exception e) {
					logger.error("Error occurred while executing completion callback", e);
				}
			}
		});

		return StreamingChatGenerator.builder()
			.startingNode(nodeName)
			.startingState(state)
			.mapResult(mapResultFunction)
			.build(wrappedFlux);
	}

	/**
	 * Create a generator with ordered notifications, ensuring sequential output: start
	 * message -> main processing -> completion message
	 * @param nodeClass node class
	 * @param state state
	 * @param mapResultFunction result mapping function
	 * @param flux response stream
	 * @param startMessage start message
	 * @param completionMessage completion message
	 * @return combined generator
	 */
	public static AsyncGenerator<? extends NodeOutput> createGeneratorWithOrderedNotifications(Class<?> nodeClass,
			OverAllState state, Function<ChatResponse, Map<String, Object>> mapResultFunction, Flux<ChatResponse> flux,
			String startMessage, String completionMessage) {

		// Create start message generator
		AsyncGenerator<? extends NodeOutput> startGenerator = createStreamPrintGenerator(startMessage);

		// Create main processing generator
		AsyncGenerator<? extends NodeOutput> mainGenerator = createGenerator(nodeClass, state, mapResultFunction, flux);

		// Create completion message generator
		AsyncGenerator<? extends NodeOutput> completionGenerator = createStreamPrintGenerator(completionMessage);

		// Combine generators to ensure sequential execution
		return new AsyncGenerator<NodeOutput>() {
			private int phase = 0; // 0: start, 1: main, 2: completion, 3: done

			private Object finalResult = null; // Save final result

			@Override
			public AsyncGenerator.Data<NodeOutput> next() {
				switch (phase) {
					case 0: // Start phase
						@SuppressWarnings("unchecked")
						AsyncGenerator.Data<NodeOutput> startData = (AsyncGenerator.Data<NodeOutput>) startGenerator
							.next();
						if (startData.isDone()) {
							phase = 1; // Switch to main processing phase
							logger.info("[{}] Start message completed, entering main processing phase",
									nodeClass.getSimpleName());
						}
						return startData;

					case 1: // Main processing phase
						@SuppressWarnings("unchecked")
						AsyncGenerator.Data<NodeOutput> mainData = (AsyncGenerator.Data<NodeOutput>) mainGenerator
							.next();
						if (mainData.isDone()) {
							// Save the result of main processing
							finalResult = mainData.resultValue().orElse(null);
							phase = 2; // Switch to completion message phase
							logger.info("[{}] Main processing completed, entering completion message phase",
									nodeClass.getSimpleName());
							// Immediately start the completion message phase, do not
							// return mainData
							return next();
						}
						return mainData;

					case 2: // Completion message phase
						@SuppressWarnings("unchecked")
						AsyncGenerator.Data<NodeOutput> completionData = (AsyncGenerator.Data<NodeOutput>) completionGenerator
							.next();
						if (completionData.isDone()) {
							phase = 3; // All done
							logger.info("[{}] Completion message output completed", nodeClass.getSimpleName());
							// After the completion message phase ends, return the final
							// completion status
							return AsyncGenerator.Data.done(finalResult);
						}
						return completionData;

					default: // All done
						return AsyncGenerator.Data.done(finalResult);
				}
			}
		};
	}

	/**
	 * Create a simple generator with a completion callback, using AsyncGenerator's
	 * composeWith mechanism
	 * @param nodeClass node class
	 * @param state state
	 * @param mapResultFunction result mapping function
	 * @param flux response stream
	 * @param completionMessage completion message
	 * @return generator with completion notification
	 */
	@SuppressWarnings("unchecked")
	public static AsyncGenerator<? extends NodeOutput> createGeneratorWithComposeCompletion(Class<?> nodeClass,
			OverAllState state, Function<ChatResponse, Map<String, Object>> mapResultFunction, Flux<ChatResponse> flux,
			String completionMessage) {

		// Create main generator
		AsyncGenerator<NodeOutput> mainGenerator = (AsyncGenerator<NodeOutput>) createGenerator(nodeClass, state,
				mapResultFunction, flux);

		// Create completion notification generator
		AsyncGenerator<NodeOutput> completionNotificationGenerator = (AsyncGenerator<NodeOutput>) createStreamPrintGenerator(
				completionMessage);

		// Use composeWith to execute completion notification after the main generator
		// completes
		return new AsyncGenerator<NodeOutput>() {
			@Override
			public AsyncGenerator.Data<NodeOutput> next() {
				AsyncGenerator.Data<NodeOutput> mainData = mainGenerator.next();

				if (mainData.isDone()) {
					// Main generator completed, compose completion notification generator
					return AsyncGenerator.Data.composeWith(completionNotificationGenerator, resultValue -> {
						logger.info("[{}] Completion notification has been output", nodeClass.getSimpleName());
					});
				}

				return mainData;
			}
		};
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
		public AsyncGenerator<? extends NodeOutput> build(Flux<ChatResponse> sourceFlux) {
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
	public static AsyncGenerator<? extends NodeOutput> createStreamingGeneratorWithMessages(Class<?> nodeClass,
			OverAllState state, String startMessage, String completionMessage,
			Function<String, Map<String, Object>> resultMapper, Flux<ChatResponse> sourceFlux) {

		return createStreamingProcessor().nodeClass(nodeClass)
			.state(state)
			.startMessage(startMessage)
			.completionMessage(completionMessage)
			.resultMapper(resultMapper)
			.build(sourceFlux);
	}

	public static AsyncGenerator<? extends NodeOutput> createStreamingGeneratorWithMessages(Class<?> nodeClass,
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

	public static AsyncGenerator<? extends NodeOutput> createStreamingGeneratorWithMessages(Class<?> nodeClass,
			OverAllState state, Function<String, Map<String, Object>> resultMapper, Flux<ChatResponse> sourceFlux,
			StreamResponseType type) {

		return createStreamingProcessor().nodeClass(nodeClass)
			.state(state)
			.type(type)
			.resultMapper(resultMapper)
			.build(sourceFlux);
	}

	public static AsyncGenerator<? extends NodeOutput> createStreamingGeneratorWithMessages(Class<?> nodeClass,
			OverAllState state, Function<String, Map<String, Object>> resultMapper, Flux<ChatResponse> sourceFlux) {

		return createStreamingProcessor().nodeClass(nodeClass)
			.state(state)
			.resultMapper(resultMapper)
			.build(sourceFlux);
	}

	/**
	 * Create multi-step processing streaming generator Supports executing multiple steps
	 * in the display process, each step has its own message and logic
	 * @param nodeClass node class
	 * @param state state
	 * @param processingSteps processing steps list
	 * @param finalResultMapper final result mapping function
	 * @return AsyncGenerator instance
	 */
	public static AsyncGenerator<? extends NodeOutput> createMultiStepGenerator(Class<?> nodeClass, OverAllState state,
			List<ProcessingStep> processingSteps,
			Function<Map<String, Object>, Map<String, Object>> finalResultMapper) {

		// Create multi-step display stream
		Flux<ChatResponse> multiStepFlux = Flux.create(emitter -> {
			try {
				Map<String, Object> stepResults = new HashMap<>();

				for (ProcessingStep step : processingSteps) {
					// Send step start message
					if (step.getMessage() != null) {
						emitter.next(ChatResponseUtil.createCustomStatusResponse(step.getMessage()));
					}

					// Execute step logic and collect results
					Map<String, Object> stepResult = step.getExecutor().apply(state);
					stepResults.putAll(stepResult);

					// Send step result message
					if (step.getResultMessage() != null) {
						String resultMsg = step.getResultMessage().apply(stepResult);
						emitter.next(ChatResponseUtil.createCustomStatusResponse(resultMsg));
					}
				}

				emitter.complete();
			}
			catch (Exception e) {
				emitter.error(e);
			}
		});

		return createStreamingProcessor().nodeClass(nodeClass)
			.state(state)
			.businessLogicExecutor(finalResultMapper != null ? currentState -> {
				// Re-execute all steps to get final result
				Map<String, Object> allResults = new HashMap<>();
				for (ProcessingStep step : processingSteps) {
					allResults.putAll(step.getExecutor().apply(currentState));
				}
				return finalResultMapper.apply(allResults);
			} : currentState -> Map.of())
			.build(multiStepFlux);
	}

	public static class ProcessingStep {

		private final String message;

		private final Function<OverAllState, Map<String, Object>> executor;

		private final Function<Map<String, Object>, String> resultMessage;

		public ProcessingStep(String message, Function<OverAllState, Map<String, Object>> executor,
				Function<Map<String, Object>, String> resultMessage) {
			this.message = message;
			this.executor = executor;
			this.resultMessage = resultMessage;
		}

		public ProcessingStep(String message, Function<OverAllState, Map<String, Object>> executor) {
			this(message, executor, null);
		}

		// Getters
		public String getMessage() {
			return message;
		}

		public Function<OverAllState, Map<String, Object>> getExecutor() {
			return executor;
		}

		public Function<Map<String, Object>, String> getResultMessage() {
			return resultMessage;
		}

		public static ProcessingStep of(String message, Function<OverAllState, Map<String, Object>> executor) {
			return new ProcessingStep(message, executor);
		}

		public static ProcessingStep of(String message, Function<OverAllState, Map<String, Object>> executor,
				Function<Map<String, Object>, String> resultMessage) {
			return new ProcessingStep(message, executor, resultMessage);
		}

	}

}

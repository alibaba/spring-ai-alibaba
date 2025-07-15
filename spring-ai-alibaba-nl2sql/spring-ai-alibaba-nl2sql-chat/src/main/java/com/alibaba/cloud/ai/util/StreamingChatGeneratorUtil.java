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
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.List;

/**
 * StreamingChatGenerator 工具类，用于创建和配置 StreamingChatGenerator 实例
 *
 * @author zhangshenghang
 */
public class StreamingChatGeneratorUtil {

	private static final Logger logger = LoggerFactory.getLogger(StreamingChatGeneratorUtil.class);

	/**
	 * 创建一个基本的 StreamingChatGenerator，使用空的 mapResult
	 * @param nodeName 节点名称
	 * @param state 状态
	 * @param flux 响应流
	 * @return AsyncGenerator 实例
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
	 * 创建一个 StreamingChatGenerator，用于流式返回打印文本
	 * @param text 要打印的文本
	 * @return AsyncGenerator 实例
	 */
	public static AsyncGenerator<? extends NodeOutput> createStreamPrintGenerator(String text) {
		AsyncGenerator<? extends NodeOutput> build = StreamingChatGenerator.builder()
			.mapResult(chatResponse -> Map.of())
			.build(Flux.just(ChatResponseUtil.createCustomStatusResponse(text)));
		return build;
	}

	/**
	 * 创建一个空的 StreamingChatGenerator，使用空的 mapResult
	 * @param flux 响应流
	 * @return AsyncGenerator 实例
	 */
	public static AsyncGenerator<? extends NodeOutput> createEmptyGenerator(Flux<ChatResponse> flux) {
		AsyncGenerator<? extends NodeOutput> build = StreamingChatGenerator.builder()
			.mapResult(chatResponse -> Map.of())
			.build(flux);
		return build;
	}

	/**
	 * 创建一个 StreamingChatGenerator，使用自定义的结果映射函数
	 * @param nodeName 节点名称
	 * @param state 状态
	 * @param mapResultFunction 结果映射函数
	 * @param flux 响应流
	 * @return AsyncGenerator 实例
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
	 * 创建一个 StreamingChatGenerator，使用类对象获取节点名称
	 * @param nodeClass 节点类
	 * @param state 状态
	 * @param flux 响应流
	 * @return AsyncGenerator 实例
	 */
	public static AsyncGenerator<? extends NodeOutput> createEmptyGenerator(Class<?> nodeClass, OverAllState state,
			Flux<ChatResponse> flux) {
		return createEmptyGenerator(nodeClass.getSimpleName(), state, flux);
	}

	/**
	 * 创建一个 StreamingChatGenerator，使用类对象获取节点名称和自定义的结果映射函数
	 * @param nodeClass 节点类
	 * @param state 状态
	 * @param mapResultFunction 结果映射函数
	 * @param flux 响应流
	 * @return AsyncGenerator 实例
	 */
	public static AsyncGenerator<? extends NodeOutput> createGenerator(Class<?> nodeClass, OverAllState state,
			Function<ChatResponse, Map<String, Object>> mapResultFunction, Flux<ChatResponse> flux) {
		return createGenerator(nodeClass.getSimpleName(), state, mapResultFunction, flux);
	}

	/**
	 * 创建一个带有完成回调的 StreamingChatGenerator 当生成器完成处理时，会执行完成回调操作
	 * @param nodeClass 节点类
	 * @param state 状态
	 * @param mapResultFunction 结果映射函数
	 * @param flux 响应流
	 * @param onCompleteCallback 完成时执行的回调函数，提供最终处理结果作为参数
	 * @return AsyncGenerator 实例
	 */
	public static AsyncGenerator<? extends NodeOutput> createGeneratorWithCallback(Class<?> nodeClass,
			OverAllState state, Function<ChatResponse, Map<String, Object>> mapResultFunction, Flux<ChatResponse> flux,
			Consumer<Map<String, Object>> onCompleteCallback) {

		// 创建一个 CompletableFuture 用于在处理完成时执行回调
		CompletableFuture<Map<String, Object>> resultFuture = new CompletableFuture<>();

		// 为最后一个元素添加信号处理
		Flux<ChatResponse> wrappedFlux = flux.doOnNext(response -> {
			try {
				// 尝试将最后一个结果保存到 future
				Map<String, Object> result = mapResultFunction.apply(response);
				resultFuture.complete(result);
			}
			catch (Exception e) {
				logger.error("处理响应时发生错误", e);
			}
		}).doFinally(signalType -> {
			if (signalType == SignalType.ON_COMPLETE) {
				try {
					// 如果 future 已经完成，获取结果并执行回调
					if (resultFuture.isDone()) {
						onCompleteCallback.accept(resultFuture.get());
					}
				}
				catch (Exception e) {
					logger.error("执行完成回调时发生错误", e);
				}
			}
		});

		// 创建并返回 AsyncGenerator
		return StreamingChatGenerator.builder()
			.startingNode(nodeClass.getSimpleName())
			.startingState(state)
			.mapResult(mapResultFunction)
			.build(wrappedFlux);
	}

	/**
	 * 创建一个带有完成回调的 StreamingChatGenerator 当生成器完成处理时，会执行完成回调操作
	 * @param nodeName 节点名称
	 * @param state 状态
	 * @param mapResultFunction 结果映射函数
	 * @param flux 响应流
	 * @param onCompleteCallback 完成时执行的回调函数，提供最终处理结果作为参数
	 * @return AsyncGenerator 实例
	 */
	public static AsyncGenerator<? extends NodeOutput> createGeneratorWithCallback(String nodeName, OverAllState state,
			Function<ChatResponse, Map<String, Object>> mapResultFunction, Flux<ChatResponse> flux,
			Consumer<Map<String, Object>> onCompleteCallback) {

		// 与上面的方法类似，只是使用提供的节点名称
		CompletableFuture<Map<String, Object>> resultFuture = new CompletableFuture<>();

		Flux<ChatResponse> wrappedFlux = flux.doOnNext(response -> {
			try {
				Map<String, Object> result = mapResultFunction.apply(response);
				resultFuture.complete(result);
			}
			catch (Exception e) {
				logger.error("处理响应时发生错误", e);
			}
		}).doFinally(signalType -> {
			if (signalType == SignalType.ON_COMPLETE) {
				try {
					if (resultFuture.isDone()) {
						onCompleteCallback.accept(resultFuture.get());
					}
				}
				catch (Exception e) {
					logger.error("执行完成回调时发生错误", e);
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
	 * 创建一个带有完成通知的生成器，确保按顺序输出：开始消息 -> 主处理 -> 完成消息
	 * @param nodeClass 节点类
	 * @param state 状态
	 * @param mapResultFunction 结果映射函数
	 * @param flux 响应流
	 * @param startMessage 开始消息
	 * @param completionMessage 完成消息
	 * @return 组合后的生成器
	 */
	public static AsyncGenerator<? extends NodeOutput> createGeneratorWithOrderedNotifications(
			Class<?> nodeClass, OverAllState state, Function<ChatResponse, Map<String, Object>> mapResultFunction,
			Flux<ChatResponse> flux, String startMessage, String completionMessage) {

		// 创建开始消息生成器
		AsyncGenerator<? extends NodeOutput> startGenerator = createStreamPrintGenerator(startMessage);
		
		// 创建主处理生成器
		AsyncGenerator<? extends NodeOutput> mainGenerator = createGenerator(nodeClass, state, mapResultFunction, flux);
		
		// 创建完成消息生成器
		AsyncGenerator<? extends NodeOutput> completionGenerator = createStreamPrintGenerator(completionMessage);

		// 组合生成器，确保按顺序执行
		return new AsyncGenerator<NodeOutput>() {
			private int phase = 0; // 0: start, 1: main, 2: completion, 3: done
			private Object finalResult = null; // 保存最终结果
			
			@Override
			public AsyncGenerator.Data<NodeOutput> next() {
				switch (phase) {
					case 0: // 开始阶段
						@SuppressWarnings("unchecked")
						AsyncGenerator.Data<NodeOutput> startData = (AsyncGenerator.Data<NodeOutput>) startGenerator.next();
						if (startData.isDone()) {
							phase = 1; // 转到主处理阶段
							logger.info("[{}] 开始消息完成，进入主处理阶段", nodeClass.getSimpleName());
						}
						return startData;
						
					case 1: // 主处理阶段
						@SuppressWarnings("unchecked")
						AsyncGenerator.Data<NodeOutput> mainData = (AsyncGenerator.Data<NodeOutput>) mainGenerator.next();
						if (mainData.isDone()) {
							// 保存主处理的结果
							finalResult = mainData.resultValue().orElse(null);
							phase = 2; // 转到完成消息阶段
							logger.info("[{}] 主处理完成，进入完成消息阶段", nodeClass.getSimpleName());
							// 立即开始完成消息阶段，不返回 mainData
							return next();
						}
						return mainData;
						
					case 2: // 完成消息阶段
						@SuppressWarnings("unchecked")
						AsyncGenerator.Data<NodeOutput> completionData = (AsyncGenerator.Data<NodeOutput>) completionGenerator.next();
						if (completionData.isDone()) {
							phase = 3; // 全部完成
							logger.info("[{}] 完成消息输出完毕", nodeClass.getSimpleName());
							// 完成消息阶段结束后，返回最终完成状态
							return AsyncGenerator.Data.done(finalResult);
						}
						return completionData;
						
					default: // 全部完成
						return AsyncGenerator.Data.done(finalResult);
				}
			}
		};
	}

	/**
	 * 创建一个简单的带完成回调的生成器，使用 AsyncGenerator 的 composeWith 机制
	 * @param nodeClass 节点类
	 * @param state 状态
	 * @param mapResultFunction 结果映射函数
	 * @param flux 响应流
	 * @param completionMessage 完成消息
	 * @return 带完成通知的生成器
	 */
	@SuppressWarnings("unchecked")
	public static AsyncGenerator<? extends NodeOutput> createGeneratorWithComposeCompletion(
			Class<?> nodeClass, OverAllState state, Function<ChatResponse, Map<String, Object>> mapResultFunction,
			Flux<ChatResponse> flux, String completionMessage) {

		// 创建主生成器
		AsyncGenerator<NodeOutput> mainGenerator = (AsyncGenerator<NodeOutput>) createGenerator(nodeClass, state, mapResultFunction, flux);
		
		// 创建完成通知生成器
		AsyncGenerator<NodeOutput> completionNotificationGenerator = (AsyncGenerator<NodeOutput>) createStreamPrintGenerator(completionMessage);

		// 使用 composeWith 在主生成器完成后执行完成通知
		return new AsyncGenerator<NodeOutput>() {
			@Override
			public AsyncGenerator.Data<NodeOutput> next() {
				AsyncGenerator.Data<NodeOutput> mainData = mainGenerator.next();
				
				if (mainData.isDone()) {
					// 主生成器完成，组合完成通知生成器
					return AsyncGenerator.Data.composeWith(completionNotificationGenerator, resultValue -> {
						logger.info("[{}] 完成通知已输出", nodeClass.getSimpleName());
					});
				}
				
				return mainData;
			}
		};
	}

	/**
	 * 通用的流式处理建造者类，支持自定义状态消息和结果收集
	 */
	public static class StreamingProcessorBuilder {
		private String startMessage;
		private String completionMessage;
		private Function<ChatResponse, String> contentExtractor = response -> response.getResult().getOutput().getText();
		private String nodeName;
		private OverAllState state;
		private Function<String, Map<String, Object>> resultMapper;
		private Function<OverAllState, Map<String, Object>> businessLogicExecutor;
		private boolean trimResult = true;
		
		private StreamingProcessorBuilder() {}
		
		/**
		 * 设置开始处理时的状态消息
		 */
		public StreamingProcessorBuilder startMessage(String startMessage) {
			this.startMessage = startMessage;
			return this;
		}
		
		/**
		 * 设置处理完成时的状态消息
		 */
		public StreamingProcessorBuilder completionMessage(String completionMessage) {
			this.completionMessage = completionMessage;
			return this;
		}
		
		/**
		 * 设置从 ChatResponse 中提取内容的方式
		 */
		public StreamingProcessorBuilder contentExtractor(Function<ChatResponse, String> contentExtractor) {
			this.contentExtractor = contentExtractor;
			return this;
		}
		
		/**
		 * 设置节点名称
		 */
		public StreamingProcessorBuilder nodeName(String nodeName) {
			this.nodeName = nodeName;
			return this;
		}
		
		/**
		 * 设置节点类（自动提取类名作为节点名称）
		 */
		public StreamingProcessorBuilder nodeClass(Class<?> nodeClass) {
			this.nodeName = nodeClass.getSimpleName();
			return this;
		}
		
		/**
		 * 设置状态
		 */
		public StreamingProcessorBuilder state(OverAllState state) {
			this.state = state;
			return this;
		}
		
		/**
		 * 设置结果映射函数（将收集到的内容转换为最终返回的Map）
		 */
		public StreamingProcessorBuilder resultMapper(Function<String, Map<String, Object>> resultMapper) {
			this.resultMapper = resultMapper;
			return this;
		}
		
		/**
		 * 设置是否对最终结果进行trim处理
		 */
		public StreamingProcessorBuilder trimResult(boolean trimResult) {
			this.trimResult = trimResult;
			return this;
		}
		
		/**
		 * 设置业务逻辑执行器（用于在mapResult中执行业务逻辑）
		 */
		public StreamingProcessorBuilder businessLogicExecutor(Function<OverAllState, Map<String, Object>> businessLogicExecutor) {
			this.businessLogicExecutor = businessLogicExecutor;
			return this;
		}
		
		/**
		 * 构建流式处理器
		 * @param sourceFlux 源数据流
		 * @return AsyncGenerator实例
		 */
		public AsyncGenerator<? extends NodeOutput> build(Flux<ChatResponse> sourceFlux) {
			if (nodeName == null || state == null) {
				throw new IllegalArgumentException("节点名称和状态不能为空");
			}
			
			// 如果有业务逻辑执行器，优先使用它
			if (businessLogicExecutor != null) {
				return createGenerator(nodeName, state, response -> businessLogicExecutor.apply(state), sourceFlux);
			}
			
			// 否则使用原有的结果映射逻辑
			if (resultMapper == null) {
				throw new IllegalArgumentException("结果映射函数不能为空");
			}
			
			// 用于收集真实处理结果
			final StringBuilder collectedResult = new StringBuilder();
			
			// 创建包装后的流
			Flux<ChatResponse> wrappedFlux = Flux.create(emitter -> {
				try {
					// 1. 发送开始消息
					if (startMessage != null && !startMessage.isEmpty()) {
						emitter.next(ChatResponseUtil.createCustomStatusResponse(startMessage));
					}
					
					// 2. 处理源数据流
					sourceFlux
						.doOnNext(response -> {
							// 提取并收集真实内容
							String content = contentExtractor.apply(response);
							if (content != null) {
								collectedResult.append(content);
							}
							// 将响应发送到流中供用户查看
							emitter.next(response);
						})
						.doOnComplete(() -> {
							// 3. 发送完成消息
							if (completionMessage != null && !completionMessage.isEmpty()) {
								emitter.next(ChatResponseUtil.createCustomStatusResponse(completionMessage));
							}
							logger.debug("[{}] 流式处理完成", nodeName);
							emitter.complete();
						})
						.doOnError(error -> {
							logger.error("[{}] 流式处理出错", nodeName, error);
							emitter.error(error);
						})
						.subscribe();
				} catch (Exception e) {
					logger.error("[{}] 流式处理启动失败", nodeName, e);
					emitter.error(e);
				}
			});
			
			// 创建生成器，使用收集到的结果
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
	 * 创建流式处理建造者实例
	 * @return StreamingProcessorBuilder实例
	 */
	public static StreamingProcessorBuilder createStreamingProcessor() {
		return new StreamingProcessorBuilder();
	}
	
	/**
	 * 快速创建带有开始和结束消息的流式生成器
	 * @param nodeClass 节点类
	 * @param state 状态
	 * @param startMessage 开始消息
	 * @param completionMessage 完成消息
	 * @param resultMapper 结果映射函数
	 * @param sourceFlux 源数据流
	 * @return AsyncGenerator实例
	 */
	public static AsyncGenerator<? extends NodeOutput> createStreamingGeneratorWithMessages(
			Class<?> nodeClass, 
			OverAllState state,
			String startMessage,
			String completionMessage,
			Function<String, Map<String, Object>> resultMapper,
			Flux<ChatResponse> sourceFlux) {
		
		return createStreamingProcessor()
			.nodeClass(nodeClass)
			.state(state)
			.startMessage(startMessage)
			.completionMessage(completionMessage)
			.resultMapper(resultMapper)
			.build(sourceFlux);
	}

	public static AsyncGenerator<? extends NodeOutput> createStreamingGeneratorWithMessages(
			Class<?> nodeClass,
			OverAllState state,
			Function<String, Map<String, Object>> resultMapper,
			Flux<ChatResponse> sourceFlux) {

		return createStreamingProcessor()
				.nodeClass(nodeClass)
				.state(state)
				.resultMapper(resultMapper)
				.build(sourceFlux);
	}

	/**
	 * 创建多阶段处理的流式生成器
	 * 支持在显示流程中执行多个步骤，每个步骤都有自己的消息和逻辑
	 * 
	 * @param nodeClass 节点类
	 * @param state 状态
	 * @param processingSteps 处理步骤列表
	 * @param finalResultMapper 最终结果映射函数
	 * @return AsyncGenerator实例
	 */
	public static AsyncGenerator<? extends NodeOutput> createMultiStepGenerator(
			Class<?> nodeClass,
			OverAllState state,
			List<ProcessingStep> processingSteps,
			Function<Map<String, Object>, Map<String, Object>> finalResultMapper) {
		
		// 创建多步骤显示流
		Flux<ChatResponse> multiStepFlux = Flux.create(emitter -> {
			try {
				Map<String, Object> stepResults = new HashMap<>();
				
				for (ProcessingStep step : processingSteps) {
					// 发送步骤开始消息
					if (step.getMessage() != null) {
						emitter.next(ChatResponseUtil.createCustomStatusResponse(step.getMessage()));
					}
					
					// 执行步骤逻辑并收集结果
					Map<String, Object> stepResult = step.getExecutor().apply(state);
					stepResults.putAll(stepResult);
					
					// 发送步骤结果消息
					if (step.getResultMessage() != null) {
						String resultMsg = step.getResultMessage().apply(stepResult);
						emitter.next(ChatResponseUtil.createCustomStatusResponse(resultMsg));
					}
				}
				
				emitter.complete();
			} catch (Exception e) {
				emitter.error(e);
			}
		});
		
		return createStreamingProcessor()
			.nodeClass(nodeClass)
			.state(state)
			.businessLogicExecutor(finalResultMapper != null ? 
				currentState -> {
					// 重新执行所有步骤以获取最终结果
					Map<String, Object> allResults = new HashMap<>();
					for (ProcessingStep step : processingSteps) {
						allResults.putAll(step.getExecutor().apply(currentState));
					}
					return finalResultMapper.apply(allResults);
				} :
				currentState -> Map.of())
			.build(multiStepFlux);
	}
	
	/**
	 * 处理步骤定义类
	 */
	public static class ProcessingStep {
		private final String message;
		private final Function<OverAllState, Map<String, Object>> executor;
		private final Function<Map<String, Object>, String> resultMessage;
		
		public ProcessingStep(String message, 
							  Function<OverAllState, Map<String, Object>> executor,
							  Function<Map<String, Object>, String> resultMessage) {
			this.message = message;
			this.executor = executor;
			this.resultMessage = resultMessage;
		}
		
		public ProcessingStep(String message, Function<OverAllState, Map<String, Object>> executor) {
			this(message, executor, null);
		}
		
		// Getters
		public String getMessage() { return message; }
		public Function<OverAllState, Map<String, Object>> getExecutor() { return executor; }
		public Function<Map<String, Object>, String> getResultMessage() { return resultMessage; }
		
		// 静态工厂方法
		public static ProcessingStep of(String message, Function<OverAllState, Map<String, Object>> executor) {
			return new ProcessingStep(message, executor);
		}
		
		public static ProcessingStep of(String message, 
										Function<OverAllState, Map<String, Object>> executor,
										Function<Map<String, Object>, String> resultMessage) {
			return new ProcessingStep(message, executor, resultMessage);
		}
	}
}

/**
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.controller;

import com.alibaba.cloud.ai.connector.config.DbConfig;
import com.alibaba.cloud.ai.constant.Constant;
import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.request.SchemaInitRequest;
import com.alibaba.cloud.ai.service.simple.SimpleVectorStoreService;
import com.alibaba.cloud.ai.service.DatasourceService;
import com.alibaba.cloud.ai.entity.Datasource;
import com.alibaba.cloud.ai.service.AgentService;
import com.alibaba.fastjson.JSON;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.alibaba.cloud.ai.constant.Constant.AGENT_ID;
import static com.alibaba.cloud.ai.constant.Constant.INPUT_KEY;
import static com.alibaba.cloud.ai.constant.Constant.RESULT;
import static com.alibaba.cloud.ai.constant.Constant.HUMAN_REVIEW_ENABLED;
import static com.alibaba.cloud.ai.constant.Constant.PLANNER_NODE_OUTPUT;

/**
 * @author zhangshenghang
 */
@RestController
@RequestMapping("nl2sql")
public class Nl2sqlForGraphController {

	private static final Logger logger = LoggerFactory.getLogger(Nl2sqlForGraphController.class);

	private final CompiledGraph compiledGraph;

	private final SimpleVectorStoreService simpleVectorStoreService;

	private final DatasourceService datasourceService;

	private final AgentService agentService;

	public Nl2sqlForGraphController(@Qualifier("nl2sqlGraph") StateGraph stateGraph,
			SimpleVectorStoreService simpleVectorStoreService, DatasourceService datasourceService,
			AgentService agentService) throws GraphStateException {
		this.compiledGraph = stateGraph.compile(CompileConfig.builder().interruptBefore("human_feedback").build());
		this.compiledGraph.setMaxIterations(100);
		this.simpleVectorStoreService = simpleVectorStoreService;
		this.datasourceService = datasourceService;
		this.agentService = agentService;
	}

	@GetMapping("/search")
	public String search(@RequestParam String query, @RequestParam String dataSetId, @RequestParam String agentId)
			throws Exception {
		// Get the data source configuration for an agent for vector initialization
		DbConfig dbConfig = getDbConfigForAgent(Integer.valueOf(agentId));

		SchemaInitRequest schemaInitRequest = new SchemaInitRequest();
		schemaInitRequest.setDbConfig(dbConfig);
		schemaInitRequest
			.setTables(Arrays.asList("categories", "order_items", "orders", "products", "users", "product_categories"));
		simpleVectorStoreService.schema(schemaInitRequest);

		boolean humanReviewEnabled = false;
		try {
			var agent = agentService.findById(Long.valueOf(agentId));
			humanReviewEnabled = agent != null && agent.getHumanReviewEnabled() != null
					&& agent.getHumanReviewEnabled() == 1;
		}
		catch (Exception ignore) {
		}

		Optional<OverAllState> invoke = compiledGraph.call(Map.of(INPUT_KEY, query, Constant.AGENT_ID, dataSetId,
				AGENT_ID, agentId, HUMAN_REVIEW_ENABLED, humanReviewEnabled));
		OverAllState overAllState = invoke.get();
		// 注意：在新的人类反馈实现中，计划内容通过流式处理发送给前端
		// 这里不再需要单独获取计划内容
		return overAllState.value(RESULT).map(Object::toString).orElse("");
	}

	@GetMapping("/init")
	public void init(@RequestParam(required = false, defaultValue = "1") Integer agentId) throws Exception {
		// Get the data source configuration for an agent for vector initialization
		DbConfig dbConfig = getDbConfigForAgent(agentId);

		SchemaInitRequest schemaInitRequest = new SchemaInitRequest();
		schemaInitRequest.setDbConfig(dbConfig);
		schemaInitRequest
			.setTables(Arrays.asList("categories", "order_items", "orders", "products", "users", "product_categories"));
		simpleVectorStoreService.schema(schemaInitRequest);
	}

	/**
	 * Get database configuration by agent ID
	 */
	private DbConfig getDbConfigForAgent(Integer agentId) {
		try {
			// Get the enabled data source for an agent
			var agentDatasources = datasourceService.getAgentDatasources(agentId);
			var activeDatasource = agentDatasources.stream()
				.filter(ad -> ad.getIsActive() == 1)
				.findFirst()
				.orElseThrow(() -> new RuntimeException("智能体 " + agentId + " 未配置启用的数据源"));

			// Convert to DbConfig
			return createDbConfigFromDatasource(activeDatasource.getDatasource());
		}
		catch (Exception e) {
			logger.error("Failed to get agent datasource config for agent: {}", agentId, e);
			throw new RuntimeException("获取智能体数据源配置失败: " + e.getMessage(), e);
		}
	}

	/**
	 * Create database configuration from data source entity
	 */
	private DbConfig createDbConfigFromDatasource(Datasource datasource) {
		DbConfig dbConfig = new DbConfig();

		// Set basic connection information
		dbConfig.setUrl(datasource.getConnectionUrl());
		dbConfig.setUsername(datasource.getUsername());
		dbConfig.setPassword(datasource.getPassword());

		// Set database type
		if ("mysql".equalsIgnoreCase(datasource.getType())) {
			dbConfig.setConnectionType("jdbc");
			dbConfig.setDialectType("mysql");
		}
		else if ("postgresql".equalsIgnoreCase(datasource.getType())) {
			dbConfig.setConnectionType("jdbc");
			dbConfig.setDialectType("postgresql");
		}
		else {
			throw new RuntimeException("不支持的数据库类型: " + datasource.getType());
		}

		// Set Schema to the database name of the data source
		dbConfig.setSchema(datasource.getDatabaseName());

		return dbConfig;
	}

	@GetMapping(value = "/stream/search", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<String>> streamSearch(@RequestParam String query, @RequestParam String agentId,
			@RequestParam(required = false) String threadId, HttpServletResponse response) throws Exception {
		// Set SSE-related HTTP headers
		response.setCharacterEncoding("UTF-8");
		response.setContentType("text/event-stream");
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Connection", "keep-alive");
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Headers", "Cache-Control");

		logger.info("Starting stream search for query: {} with agentId: {}", query, agentId);

		Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().unicast().onBackpressureBuffer();

		boolean humanReviewEnabled = false;
		try {
			var agent = agentService.findById(Long.valueOf(agentId));
			humanReviewEnabled = agent != null && agent.getHumanReviewEnabled() != null
					&& agent.getHumanReviewEnabled() == 1;
		}
		catch (Exception ignore) {
		}

		// Use streaming processing and pass agentId to the state
		// 如果没有提供threadId，生成一个
		String finalThreadId = threadId != null ? threadId : String.valueOf(System.currentTimeMillis());
		logger.info("Using threadId: {}", finalThreadId);

		Flux<NodeOutput> generator = compiledGraph.fluxStream(
				Map.of(INPUT_KEY, query, Constant.AGENT_ID, agentId, HUMAN_REVIEW_ENABLED, humanReviewEnabled),
				RunnableConfig.builder().threadId(finalThreadId).build());

		boolean finalHumanReviewEnabled = humanReviewEnabled;
		// 用于缓存人工复核计划的变量
		final StringBuilder humanReviewPlanBuilder = new StringBuilder();
		final boolean[] humanReviewDetected = { false };

		CompletableFuture.runAsync(() -> {
			try {
				generator.subscribe(output -> {
					try {
						logger.debug("Received output: {}", output.getClass().getSimpleName());
						if (output instanceof StreamingOutput) {
							StreamingOutput streamingOutput = (StreamingOutput) output;
							String chunk = streamingOutput.chunk();
							if (chunk != null && !chunk.trim().isEmpty()) {
								logger.debug("Emitting chunk: {}", chunk);

								// 如果启用了人工复核，累积所有内容
								if (finalHumanReviewEnabled) {
									humanReviewPlanBuilder.append(chunk);

									// 检查是否包含完整的计划结构
									String accumulatedContent = humanReviewPlanBuilder.toString();
									logger.debug(
											"Accumulated content length: {}, contains thought_process: {}, contains execution_plan: {}",
											accumulatedContent.length(), accumulatedContent.contains("thought_process"),
											accumulatedContent.contains("execution_plan"));

									if ((accumulatedContent.contains("thought_process")
											&& accumulatedContent.contains("execution_plan"))
											&& accumulatedContent.contains("}") && accumulatedContent.contains("]")) {
										// 检查JSON是否完整（简单检查）
										if (accumulatedContent.trim().endsWith("}")
												|| accumulatedContent.trim().endsWith("]")) {
											if (!humanReviewDetected[0]) {
												humanReviewDetected[0] = true;
												logger.info("Detected complete human review plan in streaming output");
												logger.info("Plan content length: {}", accumulatedContent.length());

												// 注意：由于图被中断，我们无法直接保存状态
												// 人类反馈将通过简单的响应处理，而不是恢复图执行

												// 发送完整的人工复核计划并结束流
												logger.info("Sending complete human review plan");
												Map<String, Object> humanReviewData = Map.of("type", "human_feedback",
														"data", accumulatedContent);
												ServerSentEvent<String> event = ServerSentEvent
													.builder(JSON.toJSONString(humanReviewData))
													.build();
												sink.tryEmitNext(event);
												sink.tryEmitComplete();
												return;
											}
										}
										else {
											logger
												.debug("JSON not complete yet, ends with: {}", accumulatedContent.trim()
													.substring(Math.max(0, accumulatedContent.trim().length() - 10)));
										}
									}
									else {
										logger.debug("Plan structure not complete yet");
									}
								}

								// Ensure that the chunk is valid JSON
								ServerSentEvent<String> event = ServerSentEvent.builder(JSON.toJSONString(chunk))
									.build();
								sink.tryEmitNext(event);
							}
							else {
								logger.warn(
										"ReceFenerator: mapResult called, finalResultived null or empty chunk from streaming output");
							}
						}
						else if (output instanceof NodeOutput) {
							logger.debug("Non-streaming output received: {}", output);

							// 检查是否是human_feedback节点
							if (finalHumanReviewEnabled && !humanReviewDetected[0]) {
								// 从累积内容中提取计划
								String accumulatedContent = humanReviewPlanBuilder.toString();
								String extractedPlanContent = extractPlanFromStreamingContent(accumulatedContent);

								if (extractedPlanContent.contains("thought_process")
										&& extractedPlanContent.contains("execution_plan")) {
									humanReviewDetected[0] = true;
									logger.debug("Found plan for human review");

									Map<String, Object> humanReviewData = Map.of("type", "human_feedback", "data",
											extractedPlanContent);
									ServerSentEvent<String> event = ServerSentEvent
										.builder(JSON.toJSONString(humanReviewData))
										.build();
									sink.tryEmitNext(event);
									sink.tryEmitComplete();
									return;
								}
								else {
									logger.info("Plan content not found in extracted content, content preview: {}",
											extractedPlanContent.length() > 200
													? extractedPlanContent.substring(0, 200) + "..."
													: extractedPlanContent);
								}
							}
							else {
								logger.debug("Human feedback check skipped: enabled={}, detected={}",
										finalHumanReviewEnabled, humanReviewDetected[0]);
							}
						}
						else {
							logger.debug("Non-streaming output received: {}", output);
						}
					}
					catch (Exception e) {
						logger.error("Error processing streaming output: ", e);
						// Do not throw exceptions; continue processing the next output
					}
				}, error -> {
					logger.error("Error in stream processing: ", error);
					// Send error event instead of throwing an error directly
					sink.tryEmitNext(ServerSentEvent.builder("error: " + error.getMessage()).event("error").build());
					sink.tryEmitComplete();
				}, () -> {
					// 流式处理完成，发送完成事件
					logger.info("Stream processing completed successfully");
					sink.tryEmitNext(ServerSentEvent.builder("complete").event("complete").build());
					sink.tryEmitComplete();
				});
			}
			catch (Exception e) {
				logger.error("Error starting stream processing: ", e);
				sink.tryEmitError(e);
			}
		});

		return sink.asFlux()
			.doOnSubscribe(subscription -> logger.info("Client subscribed to stream"))
			.doOnCancel(() -> logger.info("Client disconnected from stream"))
			.doOnError(e -> logger.error("Error occurred during streaming: ", e))
			.doOnComplete(() -> logger.info("Stream completed successfully"));
	}

	/**
	 * 从流式内容中提取计划内容
	 * 流式内容格式：{"data":"...","type":"rewrite"}{"data":"...","type":"rewrite"}...
	 * 需要提取出完整的JSON计划内容
	 */
	private String extractPlanFromStreamingContent(String streamingContent) {
		try {
			// 使用正则表达式提取所有JSON对象
			java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\{[^}]*\\}");
			java.util.regex.Matcher matcher = pattern.matcher(streamingContent);

			StringBuilder planBuilder = new StringBuilder();
			while (matcher.find()) {
				String jsonChunk = matcher.group();
				try {
					// 解析JSON对象
					@SuppressWarnings("unchecked")
					Map<String, Object> chunk = JSON.parseObject(jsonChunk, Map.class);
					String data = (String) chunk.get("data");
					if (data != null && !data.trim().isEmpty()) {
						planBuilder.append(data);
					}
				}
				catch (Exception e) {
					// 忽略解析错误的JSON块
					logger.debug("Failed to parse JSON chunk: {}", jsonChunk);
				}
			}

			String extractedContent = planBuilder.toString();
			logger.debug("Extracted content from streaming: {}", extractedContent.length());
			return extractedContent;
		}
		catch (Exception e) {
			logger.error("Error extracting plan from streaming content: ", e);
			return streamingContent; // 如果提取失败，返回原始内容
		}
	}

	/**
	 * Handle human feedback for plan review.
	 */
	@GetMapping(value = "/human-feedback", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<String>> handleHumanFeedback(@RequestParam String sessionId,
			@RequestParam String threadId, @RequestParam boolean feedBack,
			@RequestParam(required = false, defaultValue = "") String feedBackContent) throws GraphStateException {
		logger.info("Processing feedback: {} ({})", feedBack ? "approved" : "rejected",
				feedBack ? "continue" : feedBackContent);

		Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().unicast().onBackpressureBuffer();

		CompletableFuture.runAsync(() -> {
			try {
				Map<String, Object> feedbackData = Map.of("feed_back", feedBack, "feed_back_content",
						feedBackContent != null ? feedBackContent : "");
				OverAllState.HumanFeedback humanFeedback = new OverAllState.HumanFeedback(feedbackData,
						"human_feedback");

				if (feedBack) {
					// 计划通过，继续执行
					sink.tryEmitNext(ServerSentEvent.builder("执行中...").build());
					executeApprovedPlanWithResume(humanFeedback, threadId, sink);
				}
				else {
					// 计划拒绝，重新生成
					sink.tryEmitNext(ServerSentEvent.builder("重新生成中...").build());
					executeRejectedPlanWithResume(humanFeedback, threadId, sink);
				}
			}
			catch (Exception e) {
				logger.error("Error handling human feedback: ", e);
				sink.tryEmitError(e);
			}
		});

		return sink.asFlux()
			.doOnError(e -> logger.error("Human feedback stream error: ", e))
			.doOnComplete(() -> logger.debug("Human feedback stream completed"));
	}

	private void executeApprovedPlanWithResume(OverAllState.HumanFeedback humanFeedback, String threadId,
			Sinks.Many<ServerSentEvent<String>> sink) {
		try {
			Optional<OverAllState> result = compiledGraph.resume(humanFeedback,
					RunnableConfig.builder().threadId(threadId).build());

			if (result.isPresent()) {
				OverAllState finalState = result.get();
				// 处理最终结果
				Optional<String> resultValue = finalState.value(RESULT);
				if (resultValue.isPresent()) {
					ServerSentEvent<String> event = ServerSentEvent.builder(JSON.toJSONString(resultValue.get()))
						.build();
					sink.tryEmitNext(event);
				}
				sink.tryEmitNext(ServerSentEvent.builder("complete").event("complete").build());
				sink.tryEmitComplete();
			}
			else {
				sink.tryEmitNext(ServerSentEvent.builder("error: No result from approved plan execution")
					.event("error")
					.build());
				sink.tryEmitComplete();
			}
		}
		catch (Exception e) {
			logger.error("Error in approved plan resume execution: ", e);
			sink.tryEmitNext(ServerSentEvent.builder("error: " + e.getMessage()).event("error").build());
			sink.tryEmitComplete();
		}
	}

	private void executeRejectedPlanWithResume(OverAllState.HumanFeedback humanFeedback, String threadId,
			Sinks.Many<ServerSentEvent<String>> sink) {
		try {
			// 使用新的 resume 方法恢复执行
			Optional<OverAllState> result = compiledGraph.resume(humanFeedback,
					RunnableConfig.builder().threadId(threadId).build());

			if (result.isPresent()) {
				OverAllState finalState = result.get();
				// 检查是否生成了新的计划需要人工审核
				Optional<String> plannerOutputOpt = finalState.value(PLANNER_NODE_OUTPUT);
				if (plannerOutputOpt.isPresent()) {
					String currentPlanContent = plannerOutputOpt.get();
					if (currentPlanContent != null && currentPlanContent.contains("thought_process")
							&& currentPlanContent.contains("execution_plan")) {
						Map<String, Object> humanReviewData = Map.of("type", "human_feedback", "data",
								currentPlanContent);
						ServerSentEvent<String> reviewEvent = ServerSentEvent
							.builder(JSON.toJSONString(humanReviewData))
							.build();
						sink.tryEmitNext(reviewEvent);
						sink.tryEmitComplete();
						return;
					}
				}

				// 如果没有需要审核的计划，处理最终结果
				Optional<String> resultValue = finalState.value(RESULT);
				if (resultValue.isPresent()) {
					ServerSentEvent<String> event = ServerSentEvent.builder(JSON.toJSONString(resultValue.get()))
						.build();
					sink.tryEmitNext(event);
				}
				sink.tryEmitNext(ServerSentEvent.builder("complete").event("complete").build());
				sink.tryEmitComplete();
			}
			else {
				sink.tryEmitNext(ServerSentEvent.builder("error: No result from rejected plan execution")
					.event("error")
					.build());
				sink.tryEmitComplete();
			}
		}
		catch (Exception e) {
			logger.error("Error in rejected plan resume execution: ", e);
			sink.tryEmitNext(ServerSentEvent.builder("error: " + e.getMessage()).event("error").build());
			sink.tryEmitComplete();
		}
	}

}

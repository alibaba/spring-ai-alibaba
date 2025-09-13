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
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.async.AsyncGenerator;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
import static com.alibaba.cloud.ai.constant.Constant.HUMAN_REVIEW_PLAN;
import static com.alibaba.cloud.ai.constant.Constant.PLAN_VALIDATION_ERROR;

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
			AgentService agentService)
			throws GraphStateException {
		this.compiledGraph = stateGraph.compile();
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
		} catch (Exception ignore) {
		}

		Optional<OverAllState> invoke = compiledGraph
			.invoke(Map.of(INPUT_KEY, query, Constant.AGENT_ID, dataSetId, AGENT_ID, agentId, HUMAN_REVIEW_ENABLED, humanReviewEnabled));
		OverAllState overAllState = invoke.get();
		if (humanReviewEnabled) {
			var planOpt = overAllState.value(HUMAN_REVIEW_PLAN);
			if (planOpt.isPresent()) {
				return planOpt.get().toString();
			}
		}
		return overAllState.value(RESULT).map(Object::toString).orElse("");
	}

	/**
	 * 预览 Planner 生成的执行计划，供人工复核
	 */
	@GetMapping("/plan/preview")
	public String planPreview(@RequestParam String query, @RequestParam String agentId) throws Exception {
		DbConfig dbConfig = getDbConfigForAgent(Integer.valueOf(agentId));
		SchemaInitRequest schemaInitRequest = new SchemaInitRequest();
		schemaInitRequest.setDbConfig(dbConfig);
		schemaInitRequest
			.setTables(Arrays.asList("categories", "order_items", "orders", "products", "users", "product_categories"));
		simpleVectorStoreService.schema(schemaInitRequest);

		Optional<OverAllState> invoke = compiledGraph.invoke(Map.of(INPUT_KEY, query, HUMAN_REVIEW_ENABLED, true,
				Constant.AGENT_ID, agentId));
		OverAllState state = invoke.get();
		return state.value(HUMAN_REVIEW_PLAN).map(Object::toString).orElse("");
	}

	/**
	 * 提交人工反馈并继续执行
	 * decision: APPROVE / REJECT
	 * suggestion: 当 REJECT 时的修改意见
	 */
	@PostMapping("/plan/feedback")
	public String planFeedback(@RequestBody Map<String, String> body) throws Exception {
		String query = body.getOrDefault("query", "");
		String agentId = body.getOrDefault("agentId", "");
		String decision = body.getOrDefault("decision", "APPROVE");
		String suggestion = body.get("suggestion");
		DbConfig dbConfig = getDbConfigForAgent(Integer.valueOf(agentId));
		SchemaInitRequest schemaInitRequest = new SchemaInitRequest();
		schemaInitRequest.setDbConfig(dbConfig);
		schemaInitRequest
			.setTables(Arrays.asList("categories", "order_items", "orders", "products", "users", "product_categories"));
		simpleVectorStoreService.schema(schemaInitRequest);

		if ("REJECT".equalsIgnoreCase(decision)) {
			Optional<OverAllState> invoke = compiledGraph
				.invoke(Map.of(INPUT_KEY, query, Constant.AGENT_ID, agentId, PLAN_VALIDATION_ERROR,
						suggestion != null ? suggestion
								: "User rejected the plan. Please revise according to suggestions."));
			OverAllState state = invoke.get();
			return state.value(RESULT).map(Object::toString).orElse("");
		}
		else {
			Optional<OverAllState> invoke = compiledGraph.invoke(Map.of(INPUT_KEY, query, Constant.AGENT_ID, agentId));
			OverAllState state = invoke.get();
			return state.value(RESULT).map(Object::toString).orElse("");
		}
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
			HttpServletResponse response) throws Exception {
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
		} catch (Exception ignore) {
		}

		// Use streaming processing and pass agentId to the state
		AsyncGenerator<NodeOutput> generator = compiledGraph
			.stream(Map.of(INPUT_KEY, query, Constant.AGENT_ID, agentId, HUMAN_REVIEW_ENABLED, humanReviewEnabled));

		boolean finalHumanReviewEnabled = humanReviewEnabled;
		CompletableFuture.runAsync(() -> {
			try {
				generator.forEachAsync(output -> {
					try {
						logger.debug("Received output: {}", output.getClass().getSimpleName());
						if (output instanceof StreamingOutput) {
							StreamingOutput streamingOutput = (StreamingOutput) output;
							String chunk = streamingOutput.chunk();
							if (chunk != null && !chunk.trim().isEmpty()) {
								logger.debug("Emitting chunk: {}", chunk);
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
							NodeOutput nodeOutput = (NodeOutput) output;
							logger.debug("Non-streaming output received: {}", output);
						}
						else {
							logger.debug("Non-streaming output received: {}", output);
						}
					}
					catch (Exception e) {
						logger.error("Error processing streaming output: ", e);
						// Do not throw exceptions; continue processing the next output
					}
				}).thenAccept(v -> {
					// 检查最终状态是否包含人工复核计划
					try {
						// 重新执行图以获取最终状态
						Optional<OverAllState> finalState = compiledGraph
							.invoke(Map.of(INPUT_KEY, query, Constant.AGENT_ID, agentId, HUMAN_REVIEW_ENABLED, finalHumanReviewEnabled));
						
						if (finalState.isPresent()) {
							OverAllState state = finalState.get();
							var planOpt = state.value(HUMAN_REVIEW_PLAN);
							if (planOpt.isPresent()) {
								String plan = planOpt.get().toString();
								if (plan != null && !plan.trim().isEmpty()) {
									logger.info("Sending human review plan to frontend");
									// 发送人工复核计划到前端
									Map<String, Object> humanReviewData = Map.of(
										"type", "plan_generation",
										"data", plan
									);
									ServerSentEvent<String> event = ServerSentEvent.builder(JSON.toJSONString(humanReviewData))
										.build();
									sink.tryEmitNext(event);
									// 发送完成后立即关闭流，等待前端反馈
									sink.tryEmitComplete();
									return;
								}
							}
						}
					} catch (Exception e) {
						logger.error("Error checking final state for human review: ", e);
					}
					
					// Send completion event
					logger.info("Stream processing completed successfully");
					sink.tryEmitNext(ServerSentEvent.builder("complete").event("complete").build());
					sink.tryEmitComplete();
				}).exceptionally(e -> {
					logger.error("Error in stream processing: ", e);
					// Send error event instead of throwing an error directly
					sink.tryEmitNext(ServerSentEvent.builder("error: " + e.getMessage()).event("error").build());
					sink.tryEmitComplete();
					return null;
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

}

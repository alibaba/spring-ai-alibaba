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

	public Nl2sqlForGraphController(@Qualifier("nl2sqlGraph") StateGraph stateGraph,
			SimpleVectorStoreService simpleVectorStoreService, DatasourceService datasourceService)
			throws GraphStateException {
		this.compiledGraph = stateGraph.compile();
		this.compiledGraph.setMaxIterations(100);
		this.simpleVectorStoreService = simpleVectorStoreService;
		this.datasourceService = datasourceService;
	}

	@GetMapping("/search")
	public String search(@RequestParam String query, @RequestParam String dataSetId, @RequestParam String agentId)
			throws Exception {
		// 获取智能体的数据源配置用于初始化向量
		DbConfig dbConfig = getDbConfigForAgent(Integer.valueOf(agentId));

		SchemaInitRequest schemaInitRequest = new SchemaInitRequest();
		schemaInitRequest.setDbConfig(dbConfig);
		schemaInitRequest
			.setTables(Arrays.asList("categories", "order_items", "orders", "products", "users", "product_categories"));
		simpleVectorStoreService.schema(schemaInitRequest);

		Optional<OverAllState> invoke = compiledGraph
			.invoke(Map.of(INPUT_KEY, query, Constant.AGENT_ID, dataSetId, AGENT_ID, agentId));
		OverAllState overAllState = invoke.get();
		return overAllState.value(RESULT).get().toString();
	}

	@GetMapping("/init")
	public void init(@RequestParam(required = false, defaultValue = "1") Integer agentId) throws Exception {
		// 获取智能体的数据源配置用于初始化向量
		DbConfig dbConfig = getDbConfigForAgent(agentId);

		SchemaInitRequest schemaInitRequest = new SchemaInitRequest();
		schemaInitRequest.setDbConfig(dbConfig);
		schemaInitRequest
			.setTables(Arrays.asList("categories", "order_items", "orders", "products", "users", "product_categories"));
		simpleVectorStoreService.schema(schemaInitRequest);
	}

	/**
	 * 根据智能体ID获取数据库配置
	 */
	private DbConfig getDbConfigForAgent(Integer agentId) {
		try {
			// 获取智能体启用的数据源
			var agentDatasources = datasourceService.getAgentDatasources(agentId);
			var activeDatasource = agentDatasources.stream()
				.filter(ad -> ad.getIsActive() == 1)
				.findFirst()
				.orElseThrow(() -> new RuntimeException("智能体 " + agentId + " 未配置启用的数据源"));

			// 转换为 DbConfig
			return createDbConfigFromDatasource(activeDatasource.getDatasource());
		}
		catch (Exception e) {
			logger.error("Failed to get agent datasource config for agent: {}", agentId, e);
			throw new RuntimeException("获取智能体数据源配置失败: " + e.getMessage(), e);
		}
	}

	/**
	 * 从数据源实体创建数据库配置
	 */
	private DbConfig createDbConfigFromDatasource(Datasource datasource) {
		DbConfig dbConfig = new DbConfig();

		// 设置基本连接信息
		dbConfig.setUrl(datasource.getConnectionUrl());
		dbConfig.setUsername(datasource.getUsername());
		dbConfig.setPassword(datasource.getPassword());

		// 设置数据库类型
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

		// 设置Schema为数据源的数据库名称
		dbConfig.setSchema(datasource.getDatabaseName());

		return dbConfig;
	}

	@GetMapping(value = "/stream/search", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<String>> streamSearch(@RequestParam String query, @RequestParam String agentId,
			HttpServletResponse response) throws Exception {
		// 设置SSE相关的HTTP头
		response.setCharacterEncoding("UTF-8");
		response.setContentType("text/event-stream");
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Connection", "keep-alive");
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Headers", "Cache-Control");

		logger.info("Starting stream search for query: {} with agentId: {}", query, agentId);

		Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().unicast().onBackpressureBuffer();

		// 使用流式处理，传递agentId到状态中
		AsyncGenerator<NodeOutput> generator = compiledGraph
			.stream(Map.of(INPUT_KEY, query, Constant.AGENT_ID, agentId));

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
								// 确保chunk是有效的JSON
								ServerSentEvent<String> event = ServerSentEvent.builder(JSON.toJSONString(chunk))
									.build();
								sink.tryEmitNext(event);
							}
							else {
								logger.warn(
										"ReceFenerator: mapResult called, finalResultived null or empty chunk from streaming output");
							}
						}
						else {
							logger.debug("Non-streaming output received: {}", output);
						}
					}
					catch (Exception e) {
						logger.error("Error processing streaming output: ", e);
						// 不要抛出异常，继续处理下一个输出
					}
				}).thenAccept(v -> {
					// 发送完成事件
					logger.info("Stream processing completed successfully");
					sink.tryEmitNext(ServerSentEvent.builder("complete").event("complete").build());
					sink.tryEmitComplete();
				}).exceptionally(e -> {
					logger.error("Error in stream processing: ", e);
					// 发送错误事件而不是直接错误
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

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

import com.alibaba.cloud.ai.dbconnector.DbConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.async.AsyncGenerator;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.alibaba.cloud.ai.request.SchemaInitRequest;
import com.alibaba.cloud.ai.service.simple.SimpleVectorStoreService;
import com.alibaba.fastjson.JSON;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.concurrent.CompletionException;

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

	@Autowired
	private SimpleVectorStoreService simpleVectorStoreService;

	@Autowired
	private DbConfig dbConfig;

	@Autowired
	public Nl2sqlForGraphController(@Qualifier("nl2sqlGraph") StateGraph stateGraph) throws GraphStateException {
		this.compiledGraph = stateGraph.compile();
		this.compiledGraph.setMaxIterations(100);
	}

	@GetMapping("/search")
	public String search(@RequestParam String query) throws Exception {
		// 初始化向量
		SchemaInitRequest schemaInitRequest = new SchemaInitRequest();
		schemaInitRequest.setDbConfig(dbConfig);
		schemaInitRequest
			.setTables(Arrays.asList("categories", "order_items", "orders", "products", "users", "product_categories"));
		simpleVectorStoreService.schema(schemaInitRequest);

		Optional<OverAllState> invoke = compiledGraph.invoke(Map.of(INPUT_KEY, query));
		OverAllState overAllState = invoke.get();
		return overAllState.value(RESULT).get().toString();
	}

	@GetMapping("/init")
	public void init() throws Exception {
		// 初始化向量
		SchemaInitRequest schemaInitRequest = new SchemaInitRequest();
		schemaInitRequest.setDbConfig(dbConfig);
		schemaInitRequest
			.setTables(Arrays.asList("categories", "order_items", "orders", "products", "users", "product_categories"));
		simpleVectorStoreService.schema(schemaInitRequest);
	}

	@GetMapping(value = "/stream/search", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<String>> streamSearch(@RequestParam String query, HttpServletResponse response)
			throws Exception {
		response.setCharacterEncoding("UTF-8");

		Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().unicast().onBackpressureBuffer();

		// 使用流式处理
		AsyncGenerator<NodeOutput> generator = compiledGraph.stream(Map.of(INPUT_KEY, query));

		CompletableFuture.runAsync(() -> {
			generator.forEachAsync(output -> {
				try {
					// System.out.println("output = " + output);
					if (output instanceof StreamingOutput) {
						StreamingOutput streamingOutput = (StreamingOutput) output;
						String chunk = streamingOutput.chunk();
						if (chunk != null) {
							sink.tryEmitNext(ServerSentEvent.builder(JSON.toJSONString(chunk)).build());
						}
						else {
							logger.warn("Received null chunk from streaming output, skipping emission.");
						}
					}
				}
				catch (Exception e) {
					e.printStackTrace();
					throw new CompletionException(e);
				}
			}).thenAccept(v -> sink.tryEmitComplete()).exceptionally(e -> {
				logger.error("Error in stream processing", e);
				sink.tryEmitError(e);
				return null;
			});
		});

		return sink.asFlux()
			.doOnCancel(() -> System.out.println("Client disconnected from stream"))
			.doOnError(e -> System.err.println("Error occurred during streaming: " + e));
	}

}

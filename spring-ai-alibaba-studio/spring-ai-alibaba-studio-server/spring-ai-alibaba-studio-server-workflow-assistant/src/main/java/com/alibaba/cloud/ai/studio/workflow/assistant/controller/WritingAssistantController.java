/*
 * Copyright 2025-2026 the original author or authors.
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
 *
 * @author yHong
 */

package com.alibaba.cloud.ai.studio.workflow.assistant.controller;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.studio.core.observability.model.SAAGraphFlow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * @author yHong
 * @version 1.0
 * @since 2025/4/24 16:07
 */
@RestController
@RequestMapping("/write")
public class WritingAssistantController {

	private final CompiledGraph compiledGraph;
	private final StateGraph test;

	@Autowired
	public WritingAssistantController(@Qualifier("writingAssistantGraph") StateGraph writingAssistantGraph)
			throws GraphStateException {
		this.compiledGraph = writingAssistantGraph.compile();
		this.test=writingAssistantGraph;
	}
	@Bean
	public SAAGraphFlow work() {
		return SAAGraphFlow.builder()
				.id("003")
				.title("Workflow Assistant")
				.description("A workflow assistant that helps users with their workflows.")
				.ownerID("saa") // Or any owner you see fit
				.addTag("assistant")
				.addTag("workflow-generation")
				.stateGraph(test) // Pass the configured StateGraph here
				.build();
	}
	/**
	 * 调用写作助手流程图 示例请求：GET /write?text=今天我去了西湖，天气特别好，感觉特别开心
	 */
	@GetMapping
	public Map<String, Object> write(@RequestParam("text") String inputText) throws GraphRunnerException {
		var resultFuture = compiledGraph.invoke(Map.of("original_text", inputText));
		var result = resultFuture.get();
		return result.data();
	}
	@GetMapping("/test/name")
	public Object test(){
		return test.getName();
	}
	@GetMapping("/test/KeyStrategy")
	public Object test2(){
		// StateGraph doesn't have a public getKeyStrategyFactory() method
		// Instead, we can return information about the graph structure
		try {
			return test.getGraph(GraphRepresentation.Type.MERMAID, "WritingAssistant", false);
		} catch (Exception e) {
			return "Error getting graph representation: " + e.getMessage();
		}
	}

	/**
	 * 流式调用写作助手 - 基础流式输出（获取每个节点的输出）
	 * 示例请求：GET /write/stream?text=今天我去了西湖，天气特别好，感觉特别开心
	 */
	@GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<NodeOutput> writeStream(@RequestParam("text") String inputText) {
        // 使用基础流式模式，直接返回NodeOutput
        return compiledGraph.stream(Map.of("original_text", inputText));
    }

	/**
	 * 流式调用写作助手 - 快照模式（获取每个节点完成后的状态快照）
	 * 示例请求：GET /write/stream_snapshots?text=今天我去了西湖，天气特别好，感觉特别开心
	 */
	@GetMapping(path = "/stream_snapshots", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<Map<String, Object>> writeStreamSnapshots(@RequestParam("text") String inputText) {
		RunnableConfig cfg = RunnableConfig.builder()
			.streamMode(CompiledGraph.StreamMode.SNAPSHOTS)
			.build();

        return compiledGraph.stream(Map.of("original_text", inputText), cfg)
            .map(node -> node.state().data())
            .onErrorResume(e -> Flux.error(new RuntimeException("Error in snapshot stream execution: " + e.getMessage(), e)));
    }

	/**
	 * 流式调用写作助手 - 单个节点模式（只获取指定节点的输出）
	 * 示例请求：GET /write/stream_node?text=今天我去了西湖，天气特别好，感觉特别开心&nodeId=summarizer
	 */
	@GetMapping(path = "/stream_node", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<Map<String, Object>> writeStreamSingleNode(
			@RequestParam("text") String inputText,
			@RequestParam(value = "nodeId", defaultValue = "summarizer") String nodeId) {

		RunnableConfig cfg = RunnableConfig.builder()
			.streamMode(CompiledGraph.StreamMode.SNAPSHOTS)
			.build();

        return compiledGraph.stream(Map.of("original_text", inputText), cfg)
            .filter(node -> nodeId.equals(node.node()))
            .map(node -> Map.of(
                "nodeId", node.node(),
                "state", node.state().data(),
                "timestamp", System.currentTimeMillis()
            ))
            .onErrorResume(e -> Flux.error(new RuntimeException("Error in single node stream execution: " + e.getMessage(), e)));
    }
}

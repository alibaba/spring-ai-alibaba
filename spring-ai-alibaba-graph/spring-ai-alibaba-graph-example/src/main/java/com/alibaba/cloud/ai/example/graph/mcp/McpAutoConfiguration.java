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

package com.alibaba.cloud.ai.example.graph.mcp;

import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.node.McpNode;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.google.common.collect.Lists;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

@Configuration
public class McpAutoConfiguration {

	@Bean
	public ToolCallbackProvider weatherTools(OpenMeteoService openMeteoService) {
		return MethodToolCallbackProvider.builder().toolObjects(openMeteoService).build();
	}

	@Bean
	public StateGraph mcpGraph() throws GraphStateException {

		// 示例：添加 MCP Node
		McpNode mcpNode = McpNode.builder()
			.url("http://localhost:18080/sse") // MCP Server SSE 地址
			.tool("getWeatherForecastByLocation") // MCP 工具名（需根据实际 MCP Server 配置）
			.inputParamKeys(Lists.newArrayList("latitude", "longitude")) // 输入参数键
			// .param("latitude",39.9042) // 工具参数
			// .param("longitude",116.4074) // 工具参数

			.header("clientId", "111222") // 可选：添加请求头
			.outputKey("mcp_result")
			.build();

		StateGraph stateGraph = new StateGraph(() -> {
			Map<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("latitude", new ReplaceStrategy());
			strategies.put("longitude", new ReplaceStrategy());
			strategies.put("mcp_result", new ReplaceStrategy());
			return strategies;
		}).addNode("mcp_node", node_async(mcpNode)).addEdge(START, "mcp_node").addEdge("mcp_node", END);

		GraphRepresentation graphRepresentation = stateGraph.getGraph(GraphRepresentation.Type.PLANTUML, "mcp graph");

		System.out.println("\n\n");
		System.out.println(graphRepresentation.content());
		System.out.println("\n\n");

		return stateGraph;
	}

}

/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.examples.documentation.graph.examples;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * PlantUML 图表可视化示�?
 * 演示如何使用 PlantUML 可视�?Spring AI Alibaba Graph 工作流结�?
 */
public class PlantUmlExample {

	/**
	 * �?Graph 生成 PlantUML
	 */
	public static void generatePlantUmlFromGraph() throws GraphStateException {
		KeyStrategyFactory keyStrategyFactory = () -> {
			HashMap<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("result", new ReplaceStrategy());
			return strategies;
		};

		// 构建一个简单的 Graph
		StateGraph graph = new StateGraph(keyStrategyFactory)
				.addNode("step1", node_async(state -> Map.of("result", "Step 1")))
				.addNode("step2", node_async(state -> Map.of("result", "Step 2")))
				.addNode("step3", node_async(state -> Map.of("result", "Step 3")))
				.addEdge(START, "step1")
				.addEdge("step1", "step2")
				.addEdge("step2", "step3")
				.addEdge("step3", END);

		CompiledGraph compiledGraph = graph.compile();

		// 生成 PlantUML 表示
		GraphRepresentation representation = compiledGraph.getGraph(
				GraphRepresentation.Type.PLANTUML,
				"My Workflow"
		);

		// 显示 PlantUML 代码
		System.out.println("PlantUML representation:");
		System.out.println(representation.content());
	}

	/**
	 * 简�?PlantUML 代码示例
	 */
	public static void simplePlantUmlExample() {
		String code = """
				@startuml
				title Spring AI Alibaba Graph
				START --> NodeA
				NodeA --> NodeB
				NodeB --> END
				@enduml
				""";

		System.out.println("Simple PlantUML code:");
		System.out.println(code);
	}

	public static void main(String[] args) throws GraphStateException {
		System.out.println("=== PlantUML 图表可视化示�?===");
		simplePlantUmlExample();
		generatePlantUmlFromGraph();
		System.out.println("所有示例执行完�?);
	}
}


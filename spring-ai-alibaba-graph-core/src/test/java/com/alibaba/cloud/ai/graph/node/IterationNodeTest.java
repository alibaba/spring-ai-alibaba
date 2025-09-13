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
package com.alibaba.cloud.ai.graph.node;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

public class IterationNodeTest {

	private static final Logger log = LoggerFactory.getLogger(IterationNodeTest.class);

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private String runGraph(String input, NodeAction action) throws Exception {
		// 配置子图：START -> iterator -> END
		KeyStrategyFactory subFactory = () -> {
			Map<String, KeyStrategy> map = new HashMap<>();
			map.put("iterator_item", new ReplaceStrategy());
			map.put("iterator_item_result", new ReplaceStrategy());
			return map;
		};
		StateGraph subGraph = new StateGraph("iteration_graph", subFactory).addNode("iterator", node_async(action))
			.addEdge(StateGraph.START, "iterator")
			.addEdge("iterator", StateGraph.END);

		// 配置主图：START -> generate -> IterationNodeGraph -> END
		KeyStrategyFactory mainFactory = () -> {
			Map<String, KeyStrategy> map = new HashMap<>();
			map.put("input_array", new ReplaceStrategy());
			map.put("input_json_array", new ReplaceStrategy());
			map.put("iterator_item", new ReplaceStrategy());
			map.put("output_start", new ReplaceStrategy());
			map.put("iterator_item_result", new ReplaceStrategy());
			map.put("result", new ReplaceStrategy());
			map.put("output_continue", new ReplaceStrategy());
			map.put("iteration_index", new ReplaceStrategy());
			return map;
		};
		CompiledGraph graph = new StateGraph("main", mainFactory)
			.addNode("generate_array", node_async((OverAllState state) -> Map.of("input_json_array", input)))
			.addNode("iteration_node",
					IterationNode.converter()
						.inputArrayJsonKey("input_json_array")
						.tempIndexKey("iteration_index")
						.outputArrayJsonKey("result")
						.iteratorItemKey("iterator_item")
						.iteratorResultKey("iterator_item_result")
						.subGraph(subGraph)
						.convertToStateGraph())
			.addEdge(StateGraph.START, "generate_array")
			.addEdge("generate_array", "iteration_node")
			.addEdge("iteration_node", StateGraph.END)
			.compile();
		OverAllState state = graph.call(Map.of()).orElseThrow();
		return state.value("result").orElseThrow().toString();
	}

	@Test
	@DisplayName("Test Integer Iteration")
	public void testInteger() throws Exception {
		String res = this.runGraph("[1, 2, 3, 4, 5]", (OverAllState state) -> {
			int x = state.value("iterator_item", Integer.class).orElseThrow();
			int y = x * x;
			return Map.of("iterator_item_result", Integer.toString(y));
		});
		log.info("result: {}", res);
		Assertions.assertEquals(OBJECT_MAPPER.readValue(res, new TypeReference<List<String>>() {
		}), List.of("1", "4", "9", "16", "25"));
	}

	@Test
	@DisplayName("Test String Iteration")
	public void testString() throws Exception {
		String res = this.runGraph("[\"a\", \"aa\", \"aaa\", \"aaaa\", \"aaaaa\"]", (OverAllState state) -> {
			int len = state.value("iterator_item", String.class).orElseThrow().length();
			return Map.of("iterator_item_result", len);
		});
		log.info("result: {}", res);
		Assertions.assertEquals(OBJECT_MAPPER.readValue(res, new TypeReference<List<Integer>>() {
		}), List.of(1, 2, 3, 4, 5));
	}

	@Test
	@DisplayName("Test POJO Iteration")
	public void testPOJO() throws Exception {
		record Person(String name, int age) {
		}
		String res = this.runGraph("[{\"name\": \"a\", \"age\": 1}, {\"name\": \"b\", \"age\": 2}]",
				(OverAllState state) -> {
					Person p = OBJECT_MAPPER.readValue(
							OBJECT_MAPPER.writeValueAsString(state.value("iterator_item").orElseThrow()), Person.class);
					return Map.of("iterator_item_result", p.name);
				});
		log.info("result: {}", res);
		Assertions.assertEquals(OBJECT_MAPPER.readValue(res, new TypeReference<List<String>>() {
		}), List.of("a", "b"));
	}

	@Test
	@DisplayName("Test Empty Iteration")
	public void testEmpty() throws Exception {
		String res = this.runGraph("[]", (OverAllState state) -> Map.of());
		log.info("result: {}", res);
		Assertions.assertEquals(OBJECT_MAPPER.readValue(res, List.class), List.of());
	}

	@Test
	@DisplayName("Test two IterationNodes")
	public void testTwoIterationNodes() throws Exception {
		// 配置子图：START -> iterator -> END
		KeyStrategyFactory subFactory1 = () -> {
			Map<String, KeyStrategy> map = new HashMap<>();
			map.put("iterator_item", new ReplaceStrategy());
			map.put("iterator_item_result", new ReplaceStrategy());
			return map;
		};
		StateGraph subGraph1 = new StateGraph("iteration_graph", subFactory1)
			.addNode("iterator", node_async((OverAllState state) -> {
				int x = state.value("iterator_item", Integer.class).orElseThrow();
				int y = x * x;
				return Map.of("iterator_item_result", Integer.toString(y));
			}))
			.addEdge(StateGraph.START, "iterator")
			.addEdge("iterator", StateGraph.END);

		KeyStrategyFactory subFactory2 = () -> {
			Map<String, KeyStrategy> map = new HashMap<>();
			map.put("iterator_item", new ReplaceStrategy());
			map.put("iterator_item_result", new ReplaceStrategy());
			return map;
		};
		StateGraph subGraph2 = new StateGraph("iteration_graph", subFactory2)
			.addNode("iterator", node_async((OverAllState state) -> {
				int len = state.value("iterator_item", String.class).orElseThrow().length();
				return Map.of("iterator_item_result", len);
			}))
			.addEdge(StateGraph.START, "iterator")
			.addEdge("iterator", StateGraph.END);

		// 配置主图：START -> generate -> IterationNode1 -> IterationNode2 -> END
		KeyStrategyFactory mainFactory = () -> {
			Map<String, KeyStrategy> map = new HashMap<>();
			map.put("input_array", new ReplaceStrategy());
			map.put("input_json_array1", new ReplaceStrategy());
			map.put("input_json_array2", new ReplaceStrategy());
			map.put("iterator_item", new ReplaceStrategy());
			map.put("output_start", new ReplaceStrategy());
			map.put("iterator_item_result", new ReplaceStrategy());
			map.put("result1", new ReplaceStrategy());
			map.put("result2", new ReplaceStrategy());
			map.put("output_continue", new ReplaceStrategy());
			map.put("test_temp_array1", new ReplaceStrategy());
			map.put("test_temp_start1", new ReplaceStrategy());
			map.put("test_temp_end1", new ReplaceStrategy());
			map.put("test_temp_array2", new ReplaceStrategy());
			map.put("test_temp_start2", new ReplaceStrategy());
			map.put("test_temp_end2", new ReplaceStrategy());
			map.put("iteration_index1", new ReplaceStrategy());
			map.put("iteration_index2", new ReplaceStrategy());
			return map;
		};
		CompiledGraph graph = new StateGraph("main", mainFactory)
			.addNode("generate_array", node_async((OverAllState state) -> Map.of("input_json_array1", "[1, 4, 10]")))
			.addNode("iteration_node1",
					IterationNode.converter()
						.inputArrayJsonKey("input_json_array1")
						.tempIndexKey("iteration_index1")
						.outputArrayJsonKey("result1")
						.iteratorItemKey("iterator_item")
						.iteratorResultKey("iterator_item_result")
						.tempArrayKey("test_temp_array1")
						.tempStartFlagKey("test_temp_start1")
						.tempEndFlagKey("test_temp_end1")
						.subGraph(subGraph1)
						.convertToStateGraph())
			.addNode("pass", node_async((OverAllState state) -> {
				return Map.of("input_json_array2", state.value("result1", String.class).orElse("[]"));
			}))
			.addNode("iteration_node2",
					IterationNode.converter()
						.inputArrayJsonKey("input_json_array2")
						.tempIndexKey("iteration_index2")
						.outputArrayJsonKey("result2")
						.iteratorItemKey("iterator_item")
						.iteratorResultKey("iterator_item_result")
						.tempArrayKey("test_temp_array2")
						.tempStartFlagKey("test_temp_start2")
						.tempEndFlagKey("test_temp_end2")
						.subGraph(subGraph2)
						.convertToStateGraph())
			.addEdge(StateGraph.START, "generate_array")
			.addEdge("generate_array", "iteration_node1")
			.addEdge("iteration_node1", "pass")
			.addEdge("pass", "iteration_node2")
			.addEdge("iteration_node2", StateGraph.END)
			.compile();
		OverAllState state = graph.call(Map.of()).orElseThrow();
		String res = state.value("result2").orElseThrow().toString();
		log.info("result: {}", res);
		Assertions.assertEquals(OBJECT_MAPPER.readValue(res, new TypeReference<List<Integer>>() {
		}), List.of(1, 2, 3));
	}

	@Test
	@DisplayName("Test Converter.appendToStateGraph Method")
	public void testIterationAttach() throws Exception {
		StateGraph stateGraph = new StateGraph("graph",
				() -> Map.of("input_json_array", new ReplaceStrategy(), "item", new ReplaceStrategy(), "item_result",
						new ReplaceStrategy(), "result", new ReplaceStrategy(), "tv1", new ReplaceStrategy(), "tv2",
						new ReplaceStrategy(), "tv3", new ReplaceStrategy(), "tv4", new ReplaceStrategy()))
			.addNode("generate", node_async((OverAllState state) -> Map.of("input_json_array", "[1, 2, 3, 4, 5]")))
			.addNode("apply", node_async((OverAllState state) -> {
				int x = state.value("item", Integer.class).orElseThrow();
				return Map.of("item_result", x * x * x);
			}));
		// 构造迭代节点
		IterationNode.<Integer, Integer>converter()
			.subGraphStartNodeName("apply")
			.subGraphEndNodeName("apply")
			.tempArrayKey("tv1")
			.tempStartFlagKey("tv2")
			.tempEndFlagKey("tv3")
			.tempIndexKey("tv4")
			.iteratorItemKey("item")
			.iteratorResultKey("item_result")
			.inputArrayJsonKey("input_json_array")
			.outputArrayJsonKey("result")
			.appendToStateGraph(stateGraph, "iteration", "iteration_out");
		stateGraph.addNode("print", node_async((OverAllState state) -> {
			System.out.println(state.value("result", String.class).orElseThrow());
			return Map.of();
		}))
			.addEdge(StateGraph.START, "generate")
			.addEdge("generate", "iteration")
			.addEdge("iteration_out", "print")
			.addEdge("print", StateGraph.END);
		CompiledGraph compiledGraph = stateGraph.compile();
		log.info(compiledGraph.getGraph(GraphRepresentation.Type.PLANTUML, "workflow").content());
		String res = compiledGraph.call(Map.of()).orElseThrow().value("result", String.class).orElseThrow();
		log.info("result: {}", res);
		Assertions.assertEquals(OBJECT_MAPPER.readValue(res, new TypeReference<List<Integer>>() {
		}), List.of(1, 8, 27, 64, 125));
	}

}

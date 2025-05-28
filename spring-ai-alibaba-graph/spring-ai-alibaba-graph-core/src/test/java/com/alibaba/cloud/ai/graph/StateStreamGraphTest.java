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
package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.stream.LLmNodeAction;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.alibaba.cloud.ai.graph.utils.EdgeMappings;
import com.theokanning.openai.service.OpenAiService;
import org.bsc.async.AsyncGenerator;
import org.bsc.async.AsyncGeneratorQueue;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class StateStreamGraphTest {

	private String API_KEY;

	private static final Logger log = LoggerFactory.getLogger(StateStreamGraphTest.class);

	@BeforeEach
	public void init() throws GraphStateException {
		API_KEY = "xxx"; // 替换为你的API密钥
	}

	private AsyncNodeAction makeNode(String id) {
		return node_async(state -> {
			log.info("call node {}", id);
			return Map.of("messages", id);
		});
	}

	@Test
	public void testGetResultFromGenerator() throws Exception {
		var workflow = new StateGraph(() -> new OverAllState().registerKeyAndStrategy("messages", new AppendStrategy()))
			.addEdge(START, "agent_1")
			.addNode("agent_1", makeNode("agent_1"))
			.addEdge("agent_1", END);

		var app = workflow.compile();

		var iterator = app.stream(Map.of());
		for (var i : iterator) {
			System.out.println(i);
		}

		var generator = (AsyncGenerator.HasResultValue) iterator;

		System.out.println(generator.resultValue().orElse(null));

	}

	@Test
	public void testBasicNodeActionStream() throws Exception {
		StateGraph stateGraph = new StateGraph(
				() -> new OverAllState().registerKeyAndStrategy("messages", new AppendStrategy())
					.registerKeyAndStrategy("count", (oldValue, newValue) -> oldValue == null ? newValue : 1))
			.addNode("collectInput", node_async(s -> {
				// 处理输入
				String input = s.value("input", "");
				return Map.of("messages", "Received: " + input, "count", 1);
			}))
			.addNode("processData", node_async(s -> {
				// 处理数据 - 这里可以是耗时操作，会以流式方式返回结果
				final List<String> data = asList("这是", "一个", "流式", "输出", "测试");
				AtomicInteger timeOff = new AtomicInteger(1);
				final AsyncGenerator<NodeOutput> it = AsyncGenerator.collect(data.iterator(),
						(index, add) -> add.accept(of("processData", index, 500L * timeOff.getAndIncrement(), s)));
				return Map.of("messages", it);
			}))
			.addNode("generateResponse", node_async(s -> {
				// 生成最终响应
				int count = s.value("count", 0);
				return Map.of("messages", "Response generated (processed " + count + " items)", "result", "Success");
			}))
			.addEdge(START, "collectInput")
			.addEdge("collectInput", "processData")
			.addEdge("processData", "generateResponse")
			.addEdge("generateResponse", END);

		CompiledGraph compiledGraph = stateGraph.compile();
		// 初始化输入
		for (var output : compiledGraph.stream(Map.of("input", "hoho~~"))) {
			if (output instanceof AsyncGenerator<?>) {
				AsyncGenerator asyncGenerator = (AsyncGenerator) output;
				System.out.println("Streaming chunk: " + asyncGenerator);
			}
			else {
				System.out.println("Node output: " + output);
			}
		}
	}

	static CompletableFuture<NodeOutput> of(String node, String index, long delayInMills, OverAllState overAllState) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				Thread.sleep(delayInMills);
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			return new StreamingOutput(index, node, overAllState);
		});
	}

	@Test
	public void testNodeActionStreamForAsyncGeneratorQueue() throws Exception {
		StateGraph stateGraph = new StateGraph(
				() -> new OverAllState().registerKeyAndStrategy("messages", new AppendStrategy())
					.registerKeyAndStrategy("count", (oldValue, newValue) -> oldValue == null ? newValue : 1))
			.addNode("collectInput", node_async(s -> {
				// 处理输入
				String input = s.value("input", "");
				return Map.of("messages", "Received: " + input, "count", 1);
			}))
			.addNode("processData", node_async(s -> {
				// 处理数据 - 这里可以是耗时操作，会以流式方式返回结果
				BlockingQueue<AsyncGenerator.Data<StreamingOutput>> queue = new ArrayBlockingQueue<>(2000);
				AsyncGenerator.WithResult<StreamingOutput> it = new AsyncGenerator.WithResult<>(
						new AsyncGeneratorQueue.Generator<>(queue));
				String str = "random";
				new Thread(() -> {
					for (int i = 0; i < 10; i++) {
						try {
							Thread.sleep(1000);
						}
						catch (InterruptedException e) {
							throw new RuntimeException(e);
						}
						if (i == 9) {
							queue.add(AsyncGenerator.Data.done());
						}
						else {
							queue.add(AsyncGenerator.Data
								.of(new StreamingOutput(str + new Random().nextInt(10), "llmNode", s)));
						}
					}
				}).start();
				return Map.of("messages", it);
			}))
			.addNode("generateResponse", node_async(s -> {
				// 生成最终响应
				int count = s.value("count", 0);
				return Map.of("messages", "Response generated (processed " + count + " items)", "result", "Success");
			}))
			.addEdge(START, "collectInput")
			.addEdge("collectInput", "processData")
			.addEdge("processData", "generateResponse")
			.addEdge("generateResponse", END);

		CompiledGraph compiledGraph = stateGraph.compile();
		// 初始化输入
		for (var output : compiledGraph.stream(Map.of("input", "hoho~~"))) {
			if (output instanceof AsyncGenerator<?>) {
				AsyncGenerator asyncGenerator = (AsyncGenerator) output;
				System.out.println("Streaming chunk: " + asyncGenerator);
			}
			else {
				System.out.println("Node output: " + output);
			}
		}
	}

	@Test
	public void testToModelNodeActionStream() throws Exception {
		StateGraph stateGraph = new StateGraph(
				() -> new OverAllState().registerKeyAndStrategy("messages", new AppendStrategy())
					.registerKeyAndStrategy("llm_result", new AppendStrategy()))
			.addNode("llmNode", node_async(new LLmNodeAction(new OpenAiService(API_KEY, Duration.ofSeconds(30)))))
			.addNode("toolNode", node_async((t) -> Map.of("messages", "tool call result")))
			.addNode("result", node_async((t) -> Map.of("messages", "result", "llm_result", "end")))
			.addEdge(START, "llmNode")
			.addEdge("llmNode", "toolNode")
			.addEdge("toolNode", "result")
			.addEdge("result", END);

		CompiledGraph compile = stateGraph.compile();
		for (var output : compile.stream(Map.of(OverAllState.DEFAULT_INPUT_KEY, "给我写一个10字的小文章"))) {
			if (output instanceof AsyncGenerator<?>) {
				AsyncGenerator asyncGenerator = (AsyncGenerator) output;
				System.out.println("Streaming chunk: " + asyncGenerator);
			}
			else {
				System.out.println("Node output: " + output);
			}
		}
	}

	@Test
	public void testToModelNodeActionAndConditionEdgeStream() throws Exception {
		StateGraph stateGraph = new StateGraph(
				() -> new OverAllState().registerKeyAndStrategy("messages", new AppendStrategy())
					.registerKeyAndStrategy("llm_result", new AppendStrategy()))
			.addNode("llmNode", node_async(new LLmNodeAction(new OpenAiService(API_KEY, Duration.ofSeconds(30)))))
			.addNode("toolNode", node_async((t) -> Map.of("messages", "tool call result")))
			.addNode("result", node_async((t) -> Map.of("messages", "result", "llm_result", "end")))
			.addEdge(START, "llmNode")
			.addConditionalEdges("llmNode", getAsyncEdgeAction(),
					EdgeMappings.builder().to("toolNode", "toolNode").to("result", "result").toEND().build())
			.addEdge("toolNode", "result")
			.addEdge("result", END);

		CompiledGraph compile = stateGraph.compile();
		for (var output : compile.stream(Map.of(OverAllState.DEFAULT_INPUT_KEY, "给我写一个10字的小文章"))) {
			if (output instanceof AsyncGenerator<?>) {
				AsyncGenerator asyncGenerator = (AsyncGenerator) output;
				System.out.println("Streaming chunk: " + asyncGenerator);
			}
			else {
				System.out.println("Node output: " + output);
			}
		}
	}

	@NotNull
	private static AsyncEdgeAction getAsyncEdgeAction() {
		return t -> {
			if (t.value("messages").isEmpty())
				return completedFuture("result");
			List collectedMessages = (List) t.value("messages").get();
			// 使用异步方式等待流结束
			CompletableFuture<String> resultFuture = new CompletableFuture<>();
			if (!collectedMessages.isEmpty()) {
				resultFuture.complete("toolNode");
			}
			else {
				resultFuture.complete("result");
			}
			return resultFuture;
		};
	}

	@Test
	public void testStreamingOutputProcessing() throws GraphStateException {
		StateGraph stateGraph = new StateGraph(
				() -> new OverAllState().registerKeyAndStrategy("messages", new AppendStrategy())
					.registerKeyAndStrategy("count", (oldValue, newValue) -> oldValue == null ? newValue : 1))
			.addNode("collectInput", node_async(s -> {
				// 处理输入
				String input = s.value("input", "");
				return Map.of("messages", "Received: " + input, "count", 1);
			}))
			.addNode("processData", node_async(s -> {
				// 处理数据 - 这里可以是耗时操作，会以流式方式返回结果
				final List<String> data = asList("这是", "一个", "流式", "输出", "测试");
				AtomicInteger timeOff = new AtomicInteger(1);
				final AsyncGenerator<NodeOutput> it = AsyncGenerator.collect(data.iterator(),
						(index, add) -> add.accept(of("processData", index, 500L * timeOff.getAndIncrement(), s)));
				return Map.of("messages", it);
			}))
			.addNode("generateResponse", node_async(s -> {
				// 生成最终响应
				int count = s.value("count", 0);
				return Map.of("messages", "Response generated (processed " + count + " items)", "result", "Success");
			}))
			.addEdge(START, "collectInput")
			.addEdge("collectInput", "processData")
			.addEdge("processData", "generateResponse")
			.addEdge("generateResponse", END);

		CompiledGraph app = stateGraph.compile();

		// 使用辅助方法处理流式输出
		AsyncGenerator<NodeOutput> generator = app.stream(Map.of("input", "test"));
		List states = toStateList(generator);
		// 验证结果
		assertFalse(states.isEmpty(), "least one content");
		assertEquals(5, states.size(), "should be five content");
	}

	/**
	 * 处理流式输出的辅助方法
	 */
	private List<OverAllState> toStateList(AsyncGenerator<NodeOutput> generator) {
		return generator.stream().filter(s -> {
			if (s instanceof StreamingOutput streamingOutput) {
				System.out
					.println(String.format("stream data %s '%s'", streamingOutput.node(), streamingOutput.chunk()));
				return false; // 过滤掉流式输出
			}
			return true; // 保留普通节点输出
		})
			.peek(s -> System.out.println(String.format("NODE: {}", s.node())))
			.map(NodeOutput::state)
			.collect(java.util.stream.Collectors.toList());
	}

}

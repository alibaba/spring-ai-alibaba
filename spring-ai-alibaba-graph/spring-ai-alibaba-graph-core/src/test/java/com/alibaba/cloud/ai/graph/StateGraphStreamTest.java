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

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.async.AsyncGenerator;
import com.alibaba.cloud.ai.graph.async.AsyncGenerator.Data;
import com.alibaba.cloud.ai.graph.async.AsyncGeneratorQueue;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.stream.LLmNodeAction;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.alibaba.cloud.ai.graph.utils.EdgeMappings;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Test class for StateGraph streaming functionality. Verifies the correct behavior of
 * stream-based state transitions and asynchronous processing in state graphs.
 */
public class StateGraphStreamTest {

	/**
	 * API key for authentication with DashScope services
	 */
	private String API_KEY;

	/**
	 * Logger instance for tracking test execution and debugging information
	 */
	private static final Logger log = LoggerFactory.getLogger(StateGraphStreamTest.class);

	/**
	 * Test constant for specifying the Qwen Turbo model in tests
	 */
	private static final String TEST_MODEL = "qwen-turbo";

	/**
	 * Environment variable name containing the DashScope API key
	 */
	private static final String API_KEY_ENV = "AI_DASHSCOPE_API_KEY";

	/**
	 * DashScope API client instance for integration testing
	 */
	private DashScopeApi realApi;

	/**
	 * Chat options configuration used across multiple tests
	 */
	private DashScopeChatOptions options;

	/**
	 * Chat model instance configured with test-specific settings
	 */
	private DashScopeChatModel chatModel;

	/**
	 * Sets up test environment before each test method execution. Initializes API
	 * credentials and creates configured instances of test dependencies.
	 */
	@BeforeEach
	public void setUp() {
		API_KEY = System.getenv(API_KEY_ENV); // 替换为你的API密钥
		Assumptions.assumeTrue(API_KEY != null && !API_KEY.trim().isEmpty(),
				"Skipping tests because " + API_KEY_ENV + " environment variable is not set");
		// Create real API client with API key from environment
		realApi = DashScopeApi.builder().apiKey(API_KEY).build();
		// Create chat model with default options
		options = DashScopeChatOptions.builder().withModel(TEST_MODEL).build();
		chatModel = DashScopeChatModel.builder().dashScopeApi(realApi).defaultOptions(options).build();
	}

	/**
	 * Creates a basic test node with logging functionality.
	 * @param id Unique identifier for the node
	 * @return AsyncNodeAction that logs its execution and returns a simple message
	 */
	private AsyncNodeAction makeNode(String id) {
		return node_async(state -> {
			log.info("call node {}", id);
			return Map.of("messages", id);
		});
	}

	/**
	 * Tests basic generator result retrieval from the state graph. Verifies that the
	 * stream processing correctly handles terminal states and results.
	 */
	@Test
	public void testGetResultFromGenerator() throws Exception {
		var workflow = new StateGraph(() -> {
			Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
			keyStrategyMap.put("messages", new AppendStrategy());
			return keyStrategyMap;
		}).addEdge(START, "agent_1").addNode("agent_1", makeNode("agent_1")).addEdge("agent_1", END);

		var app = workflow.compile();

		var iterator = app.stream(Map.of());
		for (var i : iterator) {
			System.out.println(i);
		}

		var generator = (AsyncGenerator.HasResultValue) iterator;

		System.out.println(generator.resultValue().orElse(null));

	}

	/**
	 * Tests streaming functionality with basic node actions. Validates that the system
	 * can handle sequential node execution with streaming outputs.
	 */
	@Test
	public void testBasicNodeActionStream() throws Exception {
		StateGraph stateGraph = new StateGraph(() -> {
			Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
			keyStrategyMap.put("messages", new AppendStrategy());
			keyStrategyMap.put("count", (oldValue, newValue) -> oldValue == null ? newValue : 1);
			return keyStrategyMap;
		}).addNode("collectInput", node_async(s -> {

			String input = s.value("input", "");
			return Map.of("messages", "Received: " + input, "count", 1);
		})).addNode("processData", node_async(s -> {

			final List<String> data = asList("这是", "一个", "流式", "输出", "测试");
			AtomicInteger timeOff = new AtomicInteger(1);
			final AsyncGenerator<NodeOutput> it = AsyncGenerator.collect(data.iterator(),
					(index, add) -> add.accept(of("processData", index, 500L * timeOff.getAndIncrement(), s)));
			return Map.of("messages", it);
		})).addNode("generateResponse", node_async(s -> {

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

	/**
	 * Creates a CompletableFuture containing a StreamingOutput with delayed execution.
	 * @param node Node identifier
	 * @param index Index value for the output
	 * @param delayInMills Delay time in milliseconds
	 * @param overAllState Current state context
	 * @return CompletableFuture containing the StreamingOutput
	 */
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

	/**
	 * Tests streaming functionality using an AsyncGeneratorQueue implementation. Verifies
	 * that queue-based streaming works correctly with the state graph architecture.
	 */
	@Test
	public void testNodeActionStreamForAsyncGeneratorQueue() throws Exception {
		StateGraph stateGraph = new StateGraph(() -> {
			Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
			keyStrategyMap.put("messages", new AppendStrategy());
			keyStrategyMap.put("count", (oldValue, newValue) -> oldValue == null ? newValue : 1);
			return keyStrategyMap;
		}).addNode("collectInput", node_async(s -> {

			String input = s.value("input", "");
			return Map.of("messages", "Received: " + input, "count", 1);
		})).addNode("processData", node_async(s -> {
			AsyncGenerator.WithResult<StreamingOutput> it = getStreamingOutputWithResult(s);
			return Map.of("messages", it);
		})).addNode("generateResponse", node_async(s -> {

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

	/**
	 * Creates a streaming output generator that produces random values at intervals.
	 * @param s Current state context
	 * @return AsyncGenerator with streaming output values
	 */
	private static AsyncGenerator.WithResult<StreamingOutput> getStreamingOutputWithResult(OverAllState s) {

		BlockingQueue<Data<StreamingOutput>> queue = new ArrayBlockingQueue<>(2000);
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
					queue
						.add(AsyncGenerator.Data.of(new StreamingOutput(str + new Random().nextInt(10), "llmNode", s)));
				}
			}
		}).start();
		return it;
	}

	/**
	 * Integration test for model node action streaming. Verifies end-to-end streaming
	 * functionality with actual LLM integration.
	 */
	@Test
	@Tag("integration")
	@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
	public void testToModelNodeActionStream() throws Exception {
		StateGraph stateGraph = new StateGraph(() -> {
			Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
			keyStrategyMap.put("messages", new AppendStrategy());
			keyStrategyMap.put("llm_result", new AppendStrategy());
			return keyStrategyMap;
		}).addNode("llmNode", node_async(new LLmNodeAction(chatModel)))
			.addNode("toolNode", node_async((t) -> Map.of("messages", "tool call result")))
			.addNode("result", node_async((t) -> Map.of("messages", "result", "llm_result", "end")))
			.addEdge(START, "llmNode")
			.addEdge("llmNode", "toolNode")
			.addEdge("toolNode", "result")
			.addEdge("result", END);

		CompiledGraph compile = stateGraph.compile();
		AsyncGenerator<NodeOutput> stream = compile.stream(Map.of(OverAllState.DEFAULT_INPUT_KEY, "给我写一个10字的小文章"));
		stream.forEachAsync(nodeOutput -> System.out.println("Node output: " + nodeOutput));
	}

	/**
	 * Integration test for model node action with conditional edge routing. Verifies that
	 * streaming works correctly with dynamic path selection based on content.
	 */
	@Test
	@Tag("integration")
	@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
	public void testToModelNodeActionAndConditionEdgeStream() throws Exception {
		StateGraph stateGraph = new StateGraph(() -> {
			Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
			keyStrategyMap.put("messages", new AppendStrategy());
			keyStrategyMap.put("llm_result", new AppendStrategy());
			return keyStrategyMap;
		}).addNode("llmNode", node_async(new LLmNodeAction(chatModel)))
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

	/**
	 * Creates an asynchronous edge action for conditional routing decisions.
	 * @return AsyncEdgeAction that determines the next node based on message content
	 */
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

	/**
	 * Tests comprehensive streaming output processing pipeline. Validates that streaming
	 * outputs are properly handled and aggregated through the graph.
	 */
	@Test
	public void testStreamingOutputProcessing() throws Exception {
		StateGraph stateGraph = new StateGraph(() -> {
			Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
			keyStrategyMap.put("messages", new AppendStrategy());
			keyStrategyMap.put("count", (oldValue, newValue) -> oldValue == null ? newValue : 1);
			return keyStrategyMap;
		}).addNode("collectInput", node_async(s -> {
			// 处理输入
			String input = s.value("input", "");
			return Map.of("messages", "Received: " + input, "count", 1);
		})).addNode("processData", node_async(s -> {
			// 处理数据 - 这里可以是耗时操作，会以流式方式返回结果
			final List<String> data = asList("这是", "一个", "流式", "输出", "测试");
			AtomicInteger timeOff = new AtomicInteger(1);
			final AsyncGenerator<NodeOutput> it = AsyncGenerator.collect(data.iterator(),
					(index, add) -> add.accept(of("processData", index, 500L * timeOff.getAndIncrement(), s)));
			return Map.of("messages", it);
		})).addNode("generateResponse", node_async(s -> {
			// 生成最终响应
			int count = s.value("count", 0);
			return Map.of("messages", "Response generated (processed " + count + " items)", "result", "Success");
		}))
			.addEdge(START, "collectInput")
			.addEdge("collectInput", "processData")
			.addEdge("processData", "generateResponse")
			.addEdge("generateResponse", END);

		CompiledGraph app = stateGraph.compile();

		AsyncGenerator<NodeOutput> generator = app.stream(Map.of("input", "test"));
		List states = toStateList(generator);

		assertFalse(states.isEmpty(), "least one content");
		assertEquals(5, states.size(), "should be five content");
	}

	/**
	 * Helper method for processing streaming output. Filters out streaming chunks and
	 * extracts state information from node outputs.
	 * @param generator AsyncGenerator producing node outputs
	 * @return List of OverAllState objects representing processed states
	 */
	private List<OverAllState> toStateList(AsyncGenerator<NodeOutput> generator) {
		return generator.stream().filter(s -> {
			if (s instanceof StreamingOutput streamingOutput) {
				System.out
					.println(String.format("stream data %s '%s'", streamingOutput.node(), streamingOutput.chunk()));
				return false;
			}
			return true;
		})
			.peek(s -> System.out.println(String.format("NODE: {}", s.node())))
			.map(NodeOutput::state)
			.collect(java.util.stream.Collectors.toList());
	}

}

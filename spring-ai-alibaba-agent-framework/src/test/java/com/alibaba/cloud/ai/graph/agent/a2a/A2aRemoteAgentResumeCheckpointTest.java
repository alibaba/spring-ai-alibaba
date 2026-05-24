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
package com.alibaba.cloud.ai.graph.agent.a2a;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * <a href="https://github.com/alibaba/spring-ai-alibaba/issues/4639">#4639</a> regression for
 * {@link A2aRemoteAgent#asNode()} / {@link A2aNodeActionWithConfig#getSubGraphRunnableConfig}.
 */
class A2aRemoteAgentResumeCheckpointTest {

	@Nested
	class ResumeCheckpointRegression {

		private static final String AGENT_NAME = "qa_agent";

		@Test
		void parentInterruptBeforeA2aNode_resumeWithParentThreadId_succeeds() throws Exception {
			try (StubHttpServer http = new StubHttpServer()) {
				MemorySaver saver = MemorySaver.builder().build();
				AgentCard agentCard = buildStubAgentCard(http.baseUrl());

				A2aRemoteAgent a2aAgent = A2aRemoteAgent.builder()
						.name(AGENT_NAME)
						.description("a2a agent for #4639")
						.agentCard(agentCard)
						.instruction("echo: {input}")
						.streaming(false)
						.shareState(false)
						.compileConfig(CompileConfig.builder()
								.saverConfig(SaverConfig.builder().register(saver).build())
								.build())
						.build();

				CompiledGraph parentGraph = buildParentGraph(a2aAgent, saver);
				String threadId = "conv-a2a-" + System.nanoTime();
				RunnableConfig invokeConfig = RunnableConfig.builder().threadId(threadId).build();

				AtomicReference<NodeOutput> last = new AtomicReference<>();
				parentGraph.stream(Map.of("input", "x"), invokeConfig).doOnNext(last::set).blockLast();

				assertInstanceOf(InterruptionMetadata.class, last.get(), "应在进入 A2a 节点前中断");

				RunnableConfig resumeConfig = RunnableConfig.builder().threadId(threadId).resume().build();

				assertFalse(throwsCheckpointFailure(parentGraph, resumeConfig),
						"父 threadId resume 不应失败");
				assertFalse(innerGraphProbeThrowsCheckpoint(a2aAgent, agentCard, threadId, saver),
						"inner-graph probe 不应触发子图 checkpoint 错误");
			}
		}

		private static boolean innerGraphProbeThrowsCheckpoint(A2aRemoteAgent a2aAgent, AgentCard agentCard,
				String parentThreadId, MemorySaver saver) throws Exception {
			CompileConfig parentCompileConfig = CompileConfig.builder()
					.saverConfig(SaverConfig.builder().register(saver).build())
					.build();
			RunnableConfig parentResume = RunnableConfig.builder().threadId(parentThreadId).resume().build();

			A2aNodeActionWithConfig action = new A2aNodeActionWithConfig(new AgentCardWrapper(agentCard), AGENT_NAME,
					false, "qa_result", "echo: {input}", false, false, parentCompileConfig,
					a2aAgent.getAndCompileGraph().compileConfig);

			Method method = A2aNodeActionWithConfig.class.getDeclaredMethod("getSubGraphRunnableConfig",
					RunnableConfig.class);
			method.setAccessible(true);
			RunnableConfig childConfig = (RunnableConfig) method.invoke(action, parentResume);

			try {
				a2aAgent.getAndCompileGraph().graphResponseStream(Map.of("input", "probe"), childConfig).blockLast();
				return false;
			}
			catch (Exception ex) {
				Throwable root = ex;
				while (root.getCause() != null && root.getCause() != root) {
					root = root.getCause();
				}
				String message = root.getMessage();
				return message != null
						&& (message.contains("valid checkpoint") || message.contains("Missing Checkpoint"));
			}
		}

		private static CompiledGraph buildParentGraph(A2aRemoteAgent a2aAgent, MemorySaver saver) throws Exception {
			KeyStrategyFactory keyStrategyFactory = () -> {
				Map<String, KeyStrategy> strategies = new HashMap<>();
				strategies.put("input", new ReplaceStrategy());
				strategies.put("prep_marker", new ReplaceStrategy());
				strategies.put("qa_result", new ReplaceStrategy());
				return strategies;
			};

			StateGraph workflow = new StateGraph(keyStrategyFactory)
					.addNode("prep", node_async(state -> Map.of("prep_marker", "ok")))
					.addNode(AGENT_NAME, a2aAgent.asNode(true, false))
					.addEdge(START, "prep")
					.addEdge("prep", AGENT_NAME)
					.addEdge(AGENT_NAME, END);

			return workflow.compile(CompileConfig.builder()
					.saverConfig(SaverConfig.builder().register(saver).build())
					.interruptBefore(AGENT_NAME)
					.build());
		}

		private static AgentCard buildStubAgentCard(String url) {
			return new AgentCard.Builder()
					.name(AGENT_NAME)
					.description("stub a2a for #4639 test")
					.url(url)
					.version("1.0.0")
					.protocolVersion("0.2.5")
					.preferredTransport("JSONRPC")
					.capabilities(new AgentCapabilities.Builder().streaming(false).build())
					.defaultInputModes(List.of("text"))
					.defaultOutputModes(List.of("text"))
					.skills(List.of())
					.build();
		}

		private static boolean throwsCheckpointFailure(CompiledGraph graph, RunnableConfig resumeConfig) {
			try {
				graph.stream(null, resumeConfig).blockLast();
				return false;
			}
			catch (Exception ex) {
				Throwable root = ex;
				while (root.getCause() != null && root.getCause() != root) {
					root = root.getCause();
				}
				String message = root.getMessage();
				return message != null
						&& (message.contains("valid checkpoint") || message.contains("Missing Checkpoint"));
			}
		}

	}

	private static final class StubHttpServer implements AutoCloseable {

		private final HttpServer server;

		StubHttpServer() throws IOException {
			this.server = HttpServer.create(new InetSocketAddress(0), 0);
			this.server.createContext("/", this::handle);
			this.server.start();
		}

		String baseUrl() {
			return "http://127.0.0.1:" + server.getAddress().getPort() + "/";
		}

		private void handle(HttpExchange exchange) throws IOException {
			String body = """
					{"jsonrpc":"2.0","id":"1","result":{"kind":"message","role":"agent","parts":[{"kind":"text","text":"ok"}]}}
					""";
			byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, bytes.length);
			try (OutputStream os = exchange.getResponseBody()) {
				os.write(bytes);
			}
		}

		@Override
		public void close() {
			server.stop(0);
		}

	}

}

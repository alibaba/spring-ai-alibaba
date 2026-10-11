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
package com.alibaba.cloud.ai.graph.agent;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.OverAllStateBuilder;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Offline regression for
 * <a href="https://github.com/alibaba/spring-ai-alibaba/issues/4639">#4639</a>:
 * {@link ReactAgent#asNode()} parent-thread resume before the child namespace has a
 * checkpoint. Intentionally not gated on {@code AI_DASHSCOPE_API_KEY}.
 */
class ReactAgentResumeCheckpointTest {

	private static final String AGENT_NAME = "qa_agent";

	@Test
	void parentInterruptBeforeAgent_resumeWithParentThreadId_succeeds() throws Exception {
		MemorySaver saver = MemorySaver.builder().build();
		StubChatModel stubModel = new StubChatModel();

		ReactAgent qaAgent = ReactAgent.builder()
				.name(AGENT_NAME)
				.model(stubModel)
				.outputKey("qa_result")
				.saver(saver)
				.build();

		CompiledGraph parentGraph = buildParentGraph(qaAgent, saver);

		String threadId = "conv-001-" + System.nanoTime();
		RunnableConfig invokeConfig = RunnableConfig.builder().threadId(threadId).build();

		AtomicReference<NodeOutput> last = new AtomicReference<>();
		parentGraph.stream(Map.of("input", "x"), invokeConfig).doOnNext(last::set).blockLast();

		assertInstanceOf(InterruptionMetadata.class, last.get(), "应在进入 qa_agent 前中断");
		assertEquals(0, stubModel.callCount(), "首次中断前子图模型不应被调用");

		RunnableConfig resumeConfig = RunnableConfig.builder().threadId(threadId).resume().build();
		AtomicReference<NodeOutput> resumeLast = new AtomicReference<>();
		parentGraph.stream(null, resumeConfig).doOnNext(resumeLast::set).blockLast();

		assertTrue(stubModel.callCount() >= 1, "resume 后应真正跑进子 Agent 并调用模型");
		OverAllState state = resumeLast.get().state();
		assertEquals("ok", state.value("prep_marker").orElse(null), "父终态应保留 prep 结果");
		assertNotNull(state.value("qa_result").orElse(null), "父终态应包含子 Agent 输出");
	}

	@Test
	void parentInterruptionMetadata_beforeUnstartedChild_coldStartsSuccessfully() throws Exception {
		MemorySaver saver = MemorySaver.builder().build();
		StubChatModel stubModel = new StubChatModel();

		ReactAgent qaAgent = ReactAgent.builder()
				.name(AGENT_NAME)
				.model(stubModel)
				.outputKey("qa_result")
				.saver(saver)
				.build();

		CompiledGraph parentGraph = buildParentGraph(qaAgent, saver);
		String threadId = "conv-hitl-cold-" + System.nanoTime();

		AtomicReference<NodeOutput> last = new AtomicReference<>();
		parentGraph.stream(Map.of("input", "x"), RunnableConfig.builder().threadId(threadId).build())
				.doOnNext(last::set)
				.blockLast();
		assertInstanceOf(InterruptionMetadata.class, last.get());
		assertEquals(0, stubModel.callCount());

		// Real InterruptionMetadata on parent (HITL-style), child never ran — must not
		// reintroduce invalid-child-checkpoint failure (#4639 / review opinion #3).
		InterruptionMetadata feedback = InterruptionMetadata.builder(AGENT_NAME, OverAllStateBuilder.builder().build())
				.build();
		RunnableConfig resumeConfig = RunnableConfig.builder()
				.threadId(threadId)
				.addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, feedback)
				.build();

		AtomicReference<NodeOutput> resumeLast = new AtomicReference<>();
		parentGraph.stream(null, resumeConfig).doOnNext(resumeLast::set).blockLast();

		assertTrue(stubModel.callCount() >= 1, "冷子图应成功执行");
		assertEquals("ok", resumeLast.get().state().value("prep_marker").orElse(null));
	}

	private static CompiledGraph buildParentGraph(ReactAgent qaAgent, MemorySaver saver) throws Exception {
		KeyStrategyFactory keyStrategyFactory = () -> {
			Map<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("input", new ReplaceStrategy());
			strategies.put("prep_marker", new ReplaceStrategy());
			strategies.put("messages", new AppendStrategy());
			strategies.put("qa_result", new ReplaceStrategy());
			return strategies;
		};

		StateGraph workflow = new StateGraph(keyStrategyFactory)
				.addNode("prep", node_async(state -> Map.of("prep_marker", "ok")))
				.addNode(AGENT_NAME, qaAgent.asNode(true, false))
				.addEdge(START, "prep")
				.addEdge("prep", AGENT_NAME)
				.addEdge(AGENT_NAME, END);

		return workflow.compile(CompileConfig.builder()
				.saverConfig(SaverConfig.builder().register(saver).build())
				.interruptBefore(AGENT_NAME)
				.build());
	}

	private static final class StubChatModel implements ChatModel {

		private final AtomicInteger callCount = new AtomicInteger();

		@Override
		public ChatResponse call(Prompt prompt) {
			callCount.incrementAndGet();
			return new ChatResponse(List.of(new Generation(new AssistantMessage("ok"))));
		}

		@Override
		public Flux<ChatResponse> stream(Prompt prompt) {
			return Flux.just(call(prompt));
		}

		int callCount() {
			return callCount.get();
		}

	}

}

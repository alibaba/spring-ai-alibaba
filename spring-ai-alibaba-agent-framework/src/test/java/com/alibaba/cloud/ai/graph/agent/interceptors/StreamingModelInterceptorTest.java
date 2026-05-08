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
package com.alibaba.cloud.ai.graph.agent.interceptors;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.StreamingModelInterceptor;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import static com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants.AGENT_STATE_FOR_UPDATE_CONTEXT_KEY;
import static org.junit.jupiter.api.Assertions.*;


@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
class StreamingModelInterceptorTest extends AbstractDashscopeChatModelTest {

	/**
	 * 查询商品1的工具：触发 ReactAgent 走两轮模型调用（首轮决定调工具，第二轮基于工具返回作答）。
	 */
	static class QueryProduct1Tool implements BiFunction<QueryProduct1Tool.Request, ToolContext, String> {

		@Override
		@SuppressWarnings("unchecked")
		public String apply(Request request, ToolContext toolContext) {
			Map<String, Object> updateMap = (Map<String, Object>) toolContext.getContext()
					.get(AGENT_STATE_FOR_UPDATE_CONTEXT_KEY);
			if (updateMap != null) {
				updateMap.put("product_1_extra", "苹果手机");
			}
			return "商品1: id=1, name=苹果";
		}

		public record Request(
				@JsonProperty(required = true, value = "query")
				@JsonPropertyDescription("查询关键词") String query,

				@JsonProperty(required = true, value = "thought")
				@JsonPropertyDescription("思考为什么要调用这个工具") String thought
				) {
		}

		static ToolCallback createCallback() {
			return FunctionToolCallback.builder("query_product_1", new QueryProduct1Tool())
					.description("查询商品1的信息，返回id=1的苹果商品")
					.inputType(Request.class)
					.build();
		}
	}

	/**
	 * 验证 StreamingModelInterceptor 扩展点端到端工作：
	 * - 复用 QueryProduct1Tool，让 agent 至少触发两轮模型调用（首轮决定调用工具，第二轮基于工具返回作答）
	 * - 注册一个计数 interceptor，断言 beforeStreamCall / onStreamChunk / afterStreamComplete 全部被触发
	 * - 断言 onStreamChunk 累计 chunk 数 ≥ beforeStreamCall 次数（每轮至少 1 个 chunk）
	 * - 断言 afterStreamComplete 收到的聚合文本非空
	 */
	@Test
	void streamingModelInterceptor_shouldReceivePerChunkCallbacks() throws Exception {
		AtomicInteger beforeCount = new AtomicInteger(0);
		AtomicInteger chunkCount = new AtomicInteger(0);
		AtomicInteger completeCount = new AtomicInteger(0);
		AtomicInteger errorCount = new AtomicInteger(0);
		AtomicReference<String> lastAggregatedText = new AtomicReference<>("");

		StreamingModelInterceptor counting = new StreamingModelInterceptor() {
			@Override
			public ModelRequest beforeStreamCall(ModelRequest request) {
				int round = beforeCount.incrementAndGet();
				System.out.println("\n[interceptor] >>> beforeStreamCall round=" + round
						+ ", messages=" + (request.getMessages() == null ? 0 : request.getMessages().size())
						+ ", tools=" + request.getTools());
				return request;
			}

			@Override
			public ChatResponse onStreamChunk(ChatResponse chunk, ModelRequest request) {
				int idx = chunkCount.incrementAndGet();
				String text = null;
				boolean hasToolCalls = false;
				if (chunk != null && chunk.getResult() != null && chunk.getResult().getOutput() != null) {
					text = chunk.getResult().getOutput().getText();
					hasToolCalls = chunk.getResult().getOutput().hasToolCalls();
				}
				if (hasToolCalls) {
					System.out.println("[interceptor] chunk#" + idx + " toolCalls="
							+ chunk.getResult().getOutput().getToolCalls());
				} else {
					System.out.println("[interceptor] chunk#" + idx + " text=" + (text == null ? "<null>" : text));
				}
				return chunk;
			}

			@Override
			public void afterStreamComplete(AssistantMessage aggregatedMessage, ModelRequest request) {
				int round = completeCount.incrementAndGet();
				String aggregated = aggregatedMessage != null ? aggregatedMessage.getText() : null;
				System.out.println("[interceptor] <<< afterStreamComplete round=" + round
						+ ", aggregatedText=" + aggregated);
				if (aggregated != null) {
					lastAggregatedText.set(aggregated);
				}
			}

			@Override
			public void onStreamError(Throwable error, ModelRequest request) {
				errorCount.incrementAndGet();
				System.out.println("[interceptor] !!! onStreamError: " + error);
			}
		};

		ReactAgent agent = ReactAgent.builder()
				.name("streaming_interceptor_agent")
				.model(chatModel)
				.description("用于测试 StreamingModelInterceptor")
				.instruction("请调用 query_product_1 工具查询商品信息，并基于返回结果用 500 字内说明这是什么商品，有什么特点等。")
				.tools(List.of(QueryProduct1Tool.createCallback()))
				.streamingInterceptors(counting)
				.build();

		try {
			agent.stream("查询商品1并简要解读")
					.blockLast();

			System.out.println();
			System.out.println("===== Interceptor 计数 =====");
			System.out.println("beforeStreamCall   : " + beforeCount.get());
			System.out.println("onStreamChunk      : " + chunkCount.get());
			System.out.println("afterStreamComplete: " + completeCount.get());
			System.out.println("onStreamError      : " + errorCount.get());
			System.out.println("最后一轮聚合文本    : " + lastAggregatedText.get());

			assertTrue(beforeCount.get() >= 2,
					"agent 调用了工具，至少应触发 2 轮模型调用，beforeStreamCall 实际=" + beforeCount.get());
			assertTrue(chunkCount.get() >= beforeCount.get(),
					"每轮至少应有 1 个 chunk；chunkCount=" + chunkCount.get() + ", beforeCount=" + beforeCount.get());
			assertTrue(completeCount.get() >= 2,
					"每轮成功完成时都应触发 afterStreamComplete，实际=" + completeCount.get());
			assertEquals(0, errorCount.get(), "正常路径不应触发 onStreamError");
			assertNotNull(lastAggregatedText.get(), "聚合文本不应为 null");
			assertFalse(lastAggregatedText.get().isEmpty(), "最后一轮聚合文本不应为空（说明 chunk text 累加生效）");

		} catch (Exception ex) {
			ex.printStackTrace();
			fail("流式拦截器测试失败: " + ex.getMessage());
		}
	}
}

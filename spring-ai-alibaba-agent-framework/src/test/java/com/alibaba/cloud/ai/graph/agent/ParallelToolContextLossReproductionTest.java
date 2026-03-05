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

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.flow.agent.ParallelAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
import com.alibaba.cloud.ai.graph.agent.hook.AgentHook;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import static com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants.AGENT_STATE_FOR_UPDATE_CONTEXT_KEY;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * 复现问题：SequentialAgent 只包含一个 ParallelAgent，ParallelAgent 有两个子Agent并发查询商品，
 * 执行完成后在 SequentialAgent 的 afterHook 中获取不到子Agent通过 ToolContext 写入的数据。
 *
 * <p>场景：
 * <pre>
 * SequentialAgent (afterHook 验证数据)
 *   └── ParallelAgent
 *         ├── productAgent1 → 调用 query_product_1 工具 → 写入 product_1_extra = "苹果手机"
 *         └── productAgent2 → 调用 query_product_2 工具 → 写入 product_2_extra = "华为平板"
 * </pre>
 *
 * <p>预期：afterHook 中能读取到 product_1_extra 和 product_2_extra。
 * <p>实际：由于子图执行时 clearContext()，这些数据丢失。
 */
@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
class ParallelToolContextLossReproductionTest {

	private ChatModel chatModel;

	/** 用于在 afterHook 中记录检查结果 */
	private static final ConcurrentHashMap<String, Object> HOOK_CAPTURED_DATA = new ConcurrentHashMap<>();

	@BeforeEach
	void setUp() {
		HOOK_CAPTURED_DATA.clear();

		// Create DashScopeApi instance using the API key from environment variable
		DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(System.getenv("AI_DASHSCOPE_API_KEY")).build();

		// Create DashScope ChatModel instance
		this.chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();
	}

	private OpenAiApi createConfiguredOpenAiApi(HttpHeaders headers) {
		// 将 HttpHeaders 转换为 MultiValueMap
		MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<>();
		headers.forEach((key, values) -> headerMap.put(key, values));

		// 使用 OpenAiApi.builder() 并传入自定义的 RestClient.Builder
		return OpenAiApi.builder()
				.baseUrl("http://1688openai.alibaba-inc.com")
				.apiKey("6607aa76b08245109a406ceac465356c")
				.headers(headerMap)
				.build();
	}


	// ==================== 商品查询工具 ====================

	/**
	 * 查询商品1的工具：返回商品信息，并通过 ToolContext 写入额外数据
	 */
	static class QueryProduct1Tool implements BiFunction<QueryProduct1Tool.Request, ToolContext, String> {

		@Override
		@SuppressWarnings("unchecked")
		public String apply(Request request, ToolContext toolContext) {
			// 通过 ToolContext 写入额外状态数据
			Map<String, Object> updateMap = (Map<String, Object>) toolContext.getContext()
					.get(AGENT_STATE_FOR_UPDATE_CONTEXT_KEY);
			if (updateMap != null) {
				updateMap.put("product_1_extra", "苹果手机");
				System.out.println("[Tool query_product_1] 已写入 product_1_extra=苹果手机 到 ToolContext");
			} else {
				System.out.println("[Tool query_product_1] ⚠️ updateMap 为 null，无法写入额外数据");
			}
			return "商品1: id=1, name=苹果";
		}

		public record Request(
				@JsonProperty(required = true, value = "query")
				@JsonPropertyDescription("查询关键词") String query) {
		}

		static ToolCallback createCallback() {
			return FunctionToolCallback.builder("query_product_1", new QueryProduct1Tool())
					.description("查询商品1的信息，返回id=1的苹果商品")
					.inputType(Request.class)
					.build();
		}
	}

	/**
	 * 查询商品2的工具：返回商品信息，并通过 ToolContext 写入额外数据
	 */
	static class QueryProduct2Tool implements BiFunction<QueryProduct2Tool.Request, ToolContext, String> {

		@Override
		@SuppressWarnings("unchecked")
		public String apply(Request request, ToolContext toolContext) {
			Map<String, Object> updateMap = (Map<String, Object>) toolContext.getContext()
					.get(AGENT_STATE_FOR_UPDATE_CONTEXT_KEY);
			if (updateMap != null) {
				updateMap.put("product_2_extra", "华为平板");
				System.out.println("[Tool query_product_2] 已写入 product_2_extra=华为平板 到 ToolContext");
			} else {
				System.out.println("[Tool query_product_2] ⚠️ updateMap 为 null，无法写入额外数据");
			}
			return "商品2: id=2, name=香蕉";
		}

		public record Request(
				@JsonProperty(required = true, value = "query")
				@JsonPropertyDescription("查询关键词") String query) {
		}

		static ToolCallback createCallback() {
			return FunctionToolCallback.builder("query_product_2", new QueryProduct2Tool())
					.description("查询商品2的信息，返回id=2的香蕉商品")
					.inputType(Request.class)
					.build();
		}
	}

	// ==================== AfterHook：验证数据可见性 ====================

	/**
	 * 在 SequentialAgent 执行完成后检查 ToolContext 写入的额外数据是否可见
	 */
	static class DataVerificationHook extends AgentHook {

		@Override
		public String getName() {
			return "data_verification_hook";
		}

		@Override
		public CompletableFuture<Map<String, Object>> afterAgent(OverAllState state, RunnableConfig config) {
			System.out.println("\n╔══════════════════════════════════════════════════╗");
			System.out.println("║  [afterHook] 验证子Agent ToolContext 数据可见性    ║");
			System.out.println("╠══════════════════════════════════════════════════╣");

			Optional<Object> extra1 = state.value("product_1_extra");
			Optional<Object> extra2 = state.value("product_2_extra");

			System.out.println("║  product_1_extra: " + (extra1.isPresent() ? extra1.get() : "❌ 缺失"));
			System.out.println("║  product_2_extra: " + (extra2.isPresent() ? extra2.get() : "❌ 缺失"));

			// 记录到静态 map，供断言使用
			HOOK_CAPTURED_DATA.put("product_1_extra_present", extra1.isPresent());
			HOOK_CAPTURED_DATA.put("product_2_extra_present", extra2.isPresent());
			extra1.ifPresent(value -> HOOK_CAPTURED_DATA.put("product_1_extra_value", value));
			extra2.ifPresent(value -> HOOK_CAPTURED_DATA.put("product_2_extra_value", value));

			// 打印所有 state keys 帮助调试
			System.out.println("║  所有 state keys: " + state.data().keySet());
			System.out.println("╚══════════════════════════════════════════════════╝\n");

			return CompletableFuture.completedFuture(Map.of());
		}
	}

	// ==================== 测试用例 ====================

	/**
	 * 复现：SequentialAgent(afterHook) → ParallelAgent(2个子Agent并发查询商品)
	 * 验证 afterHook 能否获取到子Agent通过 ToolContext 写入的额外数据
	 */
	@Test
	void testToolContextDataLossInParallelSubAgents() throws Exception {
		// 子Agent1：查询商品1
		ReactAgent productAgent1 = ReactAgent.builder()
				.name("product_agent_1")
				.model(chatModel)
				.description("查询商品1")
				.instruction("请调用 query_product_1 工具查询商品信息。直接调用工具，不需要额外说明。")
				.tools(List.of(QueryProduct1Tool.createCallback()))
				.outputKey("product_1_result")
				.build();

		// 子Agent2：查询商品2
		ReactAgent productAgent2 = ReactAgent.builder()
				.name("product_agent_2")
				.model(chatModel)
				.description("查询商品2")
				.instruction("请调用 query_product_2 工具查询商品信息。直接调用工具，不需要额外说明。")
				.tools(List.of(QueryProduct2Tool.createCallback()))
				.outputKey("product_2_result")
				.build();

		// ParallelAgent：并发执行两个商品查询
		ParallelAgent parallelAgent = ParallelAgent.builder()
				.name("parallel_product_query")
				.description("并发查询两个商品")
				.subAgents(List.of(productAgent1, productAgent2))
				.build();

		// SequentialAgent：包含 ParallelAgent + afterHook 验证数据
		SequentialAgent sequentialAgent = SequentialAgent.builder()
				.name("product_query_workflow")
				.description("商品查询工作流")
				.subAgents(List.of(parallelAgent))
				.hooks(List.of(new DataVerificationHook()))
				.build();

		try {
			Optional<OverAllState> result = sequentialAgent.invoke("查询所有商品");

			assertTrue(result.isPresent(), "执行结果不应为空");

			OverAllState finalState = result.get();
			System.out.println("\n===== 最终 State =====");
			System.out.println("所有 keys: " + finalState.data().keySet());
			finalState.data().forEach((key, value) ->
					System.out.println("  " + key + " = " + value));

			// 验证 afterHook 是否捕获到了数据
			Boolean extra1Present = (Boolean) HOOK_CAPTURED_DATA.get("product_1_extra_present");
			Boolean extra2Present = (Boolean) HOOK_CAPTURED_DATA.get("product_2_extra_present");

			System.out.println("\n===== 验证结果 =====");
			System.out.println("afterHook 中 product_1_extra 是否存在: " + extra1Present);
			System.out.println("afterHook 中 product_2_extra 是否存在: " + extra2Present);

			// 这两个断言预期会失败，从而复现问题
			assertTrue(extra1Present != null && extra1Present,
					"afterHook 应该能获取到 product_1_extra（通过 ToolContext 写入的数据），但实际获取不到");
			assertTrue(extra2Present != null && extra2Present,
					"afterHook 应该能获取到 product_2_extra（通过 ToolContext 写入的数据），但实际获取不到");

		} catch (java.util.concurrent.CompletionException completionException) {
			completionException.printStackTrace();
			fail("执行失败: " + completionException.getMessage());
		}
	}
}

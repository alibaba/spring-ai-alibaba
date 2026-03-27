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

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.flow.agent.ParallelAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
import com.alibaba.cloud.ai.graph.agent.hook.AgentHook;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.http.HttpHeaders;

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
 * Reproduces the issue: SequentialAgent contains only one ParallelAgent, which has two sub-agents concurrently querying products.
 * After execution completes, the data written by sub-agents through ToolContext cannot be retrieved in SequentialAgent's afterHook.
 *
 * <p>Scenario:
 * <pre>
 * SequentialAgent (afterHook verifies data)
 *   └── ParallelAgent
 *         ├── productAgent1 → calls query_product_1 tool → writes product_1_extra = "苹果手机"
 *         └── productAgent2 → calls query_product_2 tool → writes product_2_extra = "华为平板"
 * </pre>
 *
 * <p>Expected: afterHook should be able to read product_1_extra and product_2_extra.
 * <p>Actual: These data are lost due to clearContext() during sub-graph execution.
 */
class ParallelToolContextLossReproductionTest {

	private ChatModel chatModel;

	/** Used to record verification results in afterHook */
	private static final ConcurrentHashMap<String, Object> HOOK_CAPTURED_DATA = new ConcurrentHashMap<>();

	@BeforeEach
	void setUp() {
		HOOK_CAPTURED_DATA.clear();

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.add("X-PLATFORM", "dashscope");

		OpenAiApi openAiApi = OpenAiApi.builder()
				.apiKey("6607aa76b08245109a406ceac465356c")
				.baseUrl("http://1688openai.alibaba-inc.com")
				.headers(httpHeaders)
				.build();

		this.chatModel = OpenAiChatModel.builder()
				.openAiApi(openAiApi)
				.defaultOptions(
						OpenAiChatOptions.builder()
								.model("qwen3-next-80b-a3b-instruct")
								.build()
				)
				.build();
	}

	// ==================== Product Query Tools ====================

	/**
	 * Tool for querying product 1: returns product information and writes additional data through ToolContext
	 */
	static class QueryProduct1Tool implements BiFunction<QueryProduct1Tool.Request, ToolContext, String> {

		@Override
		@SuppressWarnings("unchecked")
		public String apply(Request request, ToolContext toolContext) {
			// Write additional state data through ToolContext
			Map<String, Object> updateMap = (Map<String, Object>) toolContext.getContext()
					.get(AGENT_STATE_FOR_UPDATE_CONTEXT_KEY);
			if (updateMap != null) {
				updateMap.put("product_1_extra", "苹果手机");
				System.out.println("[Tool query_product_1] Written product_1_extra=苹果手机 to ToolContext");
			} else {
				System.out.println("[Tool query_product_1] updateMap is null, cannot write additional data");
			}
			return "Product 1: id=1, name=苹果";
		}

		public record Request(
				@JsonProperty(required = true, value = "query")
				@JsonPropertyDescription("Query keyword") String query) {
		}

		static ToolCallback createCallback() {
			return FunctionToolCallback.builder("query_product_1", new QueryProduct1Tool())
					.description("Query information for product 1, returns apple product with id=1")
					.inputType(Request.class)
					.build();
		}
	}

	/**
	 * Tool for querying product 2: returns product information and writes additional data through ToolContext
	 */
	static class QueryProduct2Tool implements BiFunction<QueryProduct2Tool.Request, ToolContext, String> {

		@Override
		@SuppressWarnings("unchecked")
		public String apply(Request request, ToolContext toolContext) {
			Map<String, Object> updateMap = (Map<String, Object>) toolContext.getContext()
					.get(AGENT_STATE_FOR_UPDATE_CONTEXT_KEY);
			if (updateMap != null) {
				updateMap.put("product_2_extra", "华为平板");
				System.out.println("[Tool query_product_2] Written product_2_extra=华为平板 to ToolContext");
			} else {
				System.out.println("[Tool query_product_2] updateMap is null, cannot write additional data");
			}
			return "Product 2: id=2, name=香蕉";
		}

		public record Request(
				@JsonProperty(required = true, value = "query")
				@JsonPropertyDescription("Query keyword") String query) {
		}

		static ToolCallback createCallback() {
			return FunctionToolCallback.builder("query_product_2", new QueryProduct2Tool())
					.description("Query information for product 2, returns banana product with id=2")
					.inputType(Request.class)
					.build();
		}
	}

	// ==================== AfterHook: Verify Data Visibility ====================

	/**
	 * Checks whether additional data written through ToolContext is visible after SequentialAgent execution completes
	 */
	static class DataVerificationHook extends AgentHook {

		@Override
		public String getName() {
			return "data_verification_hook";
		}

		@Override
		public CompletableFuture<Map<String, Object>> afterAgent(OverAllState state, RunnableConfig config) {
			System.out.println("\n╔══════════════════════════════════════════════════╗");
			System.out.println("║  [afterHook] Verify Sub-Agent ToolContext Data Visibility    ║");
			System.out.println("╠══════════════════════════════════════════════════╣");

			Optional<Object> extra1 = state.value("product_1_extra");
			Optional<Object> extra2 = state.value("product_2_extra");

			System.out.println("║  product_1_extra: " + (extra1.isPresent() ? extra1.get() : "Missing"));
			System.out.println("║  product_2_extra: " + (extra2.isPresent() ? extra2.get() : "Missing"));

			// Record to static map for assertions
			HOOK_CAPTURED_DATA.put("product_1_extra_present", extra1.isPresent());
			HOOK_CAPTURED_DATA.put("product_2_extra_present", extra2.isPresent());
			extra1.ifPresent(value -> HOOK_CAPTURED_DATA.put("product_1_extra_value", value));
			extra2.ifPresent(value -> HOOK_CAPTURED_DATA.put("product_2_extra_value", value));

			// Print all state keys to help with debugging
			System.out.println("║  All state keys: " + state.data().keySet());
			System.out.println("╚══════════════════════════════════════════════════╝\n");

			return CompletableFuture.completedFuture(Map.of());
		}
	}

	// ==================== Test Cases ====================

	/**
	 * Reproduces: SequentialAgent(afterHook) → ParallelAgent(2 sub-agents concurrently query products)
	 * Verifies whether afterHook can retrieve additional data written by sub-agents through ToolContext
	 */
	@Test
	void testToolContextDataLossInParallelSubAgents() throws Exception {
		// Sub-agent 1: Query product 1
		ReactAgent productAgent1 = ReactAgent.builder()
				.name("product_agent_1")
				.model(chatModel)
				.description("Query product 1")
				.instruction("Please call the query_product_1 tool to query product information. Call the tool directly, no additional explanation needed.")
				.tools(List.of(QueryProduct1Tool.createCallback()))
				.outputKey("product_1_result")
				.build();

		// Sub-agent 2: Query product 2
		ReactAgent productAgent2 = ReactAgent.builder()
				.name("product_agent_2")
				.model(chatModel)
				.description("Query product 2")
				.instruction("Please call the query_product_2 tool to query product information. Call the tool directly, no additional explanation needed.")
				.tools(List.of(QueryProduct2Tool.createCallback()))
				.outputKey("product_2_result")
				.build();

		// ParallelAgent: Execute two product queries concurrently
		ParallelAgent parallelAgent = ParallelAgent.builder()
				.name("parallel_product_query")
				.description("Query two products concurrently")
				.subAgents(List.of(productAgent1, productAgent2))
				.build();

		// SequentialAgent: Contains ParallelAgent + afterHook to verify data
		SequentialAgent sequentialAgent = SequentialAgent.builder()
				.name("product_query_workflow")
				.description("Product query workflow")
				.subAgents(List.of(parallelAgent))
				.hooks(List.of(new DataVerificationHook()))
				.build();

		try {
			Optional<OverAllState> result = sequentialAgent.invoke("Query all products");

			assertTrue(result.isPresent(), "Execution result should not be empty");

			OverAllState finalState = result.get();
			System.out.println("\n===== Final State =====");
			System.out.println("All keys: " + finalState.data().keySet());
			finalState.data().forEach((key, value) ->
					System.out.println("  " + key + " = " + value));

			// Verify whether afterHook captured the data
			Boolean extra1Present = (Boolean) HOOK_CAPTURED_DATA.get("product_1_extra_present");
			Boolean extra2Present = (Boolean) HOOK_CAPTURED_DATA.get("product_2_extra_present");

			System.out.println("\n===== Verification Results =====");
			System.out.println("Is product_1_extra present in afterHook: " + extra1Present);
			System.out.println("Is product_2_extra present in afterHook: " + extra2Present);

			// These two assertions are expected to fail, thereby reproducing the issue
			assertTrue(extra1Present != null && extra1Present,
					"afterHook should be able to retrieve product_1_extra (data written through ToolContext), but actually cannot retrieve it");
			assertTrue(extra2Present != null && extra2Present,
					"afterHook should be able to retrieve product_2_extra (data written through ToolContext), but actually cannot retrieve it");

		} catch (java.util.concurrent.CompletionException completionException) {
			completionException.printStackTrace();
			fail("Execution failed: " + completionException.getMessage());
		}
	}
}

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
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.tools.ToolContextHelper;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.concurrent.atomic.AtomicReference;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;

/**
 * ToolContextHelper Example - Demonstrates metadata access via ToolContextHelper.
 *
 * <p>This example validates that ToolContextHelper works correctly for accessing
 * metadata from ToolContext in a real agent scenario:</p>
 * <ol>
 *   <li>Tool is invoked by Agent through LLM</li>
 *   <li>Tool retrieves metadata using ToolContextHelper</li>
 *   <li>Tool stores validation result in static holder</li>
 *   <li>Test code retrieves and validates the result</li>
 * </ol>
 *
 * <p>How to run:</p>
 * <pre>
 * export AI_DASHSCOPE_API_KEY=your-api-key
 * mvn compile exec:java -Dexec.mainClass="com.alibaba.cloud.ai.examples.studiocorsconditionalit.ToolContextHelperExample"
 * </pre>
 *
 * @see ToolContextHelper
 */
public class ToolContextHelperExample {

	private static final Logger log = LoggerFactory.getLogger(ToolContextHelperExample.class);

	/**
	 * Internal key constants (not exposed as public API)
	 */
	private static final String AGENT_CONFIG_CONTEXT_KEY = "_AGENT_CONFIG_";
	private static final String AGENT_STATE_CONTEXT_KEY = "_AGENT_STATE_";
	private static final String AGENT_STATE_FOR_UPDATE_CONTEXT_KEY = "_AGENT_STATE_FOR_UPDATE_";

	/**
	 * Validation result record
	 */
	public static class ValidationResult {

		public boolean helperWayWorks = false;

		public boolean directWayWorks = false;

		public boolean dataConsistent = false;

		public String helperConfigValue = null;

		public String directConfigValue = null;

		public String errorMessage = null;

		@Override
		public String toString() {
			return String.format(
					"ValidationResult{helperWayWorks=%s, directWayWorks=%s, dataConsistent=%s, "
							+ "helperConfigValue='%s', directConfigValue='%s', error='%s'}",
					helperWayWorks, directWayWorks, dataConsistent, helperConfigValue, directConfigValue, errorMessage);
		}

	}

	/**
	 * Metadata validation tool using both Helper and direct key access.
	 *
	 * <p>This tool demonstrates the dual access pattern:</p>
	 * <ol>
	 *   <li>Helper way - Using ToolContextHelper (recommended)</li>
	 *   <li>Direct key way - Using internal keys directly</li>
	 *   <li>Compare results from both approaches</li>
	 *   <li>Store result in static holder for test retrieval</li>
	 * </ol>
	 */
	public static class MetadataValidationTool
			implements BiFunction<MetadataValidationTool.Request, ToolContext, MetadataValidationTool.Response> {

		/**
		 * Static holder to pass results back to test code after tool execution
		 */
		private static final AtomicReference<ValidationResult> LAST_RESULT_HOLDER = new AtomicReference<>();

		/**
		 * Get the last tool execution result
		 */
		public static ValidationResult getLastResult() {
			return LAST_RESULT_HOLDER.get();
		}

		/**
		 * Clear the previous result
		 */
		public static void clearLastResult() {
			LAST_RESULT_HOLDER.set(null);
		}

		@Override
		public Response apply(Request request, ToolContext toolContext) {
			ValidationResult result = new ValidationResult();

			try {
				log.info("[Tool Execution] Starting ToolContext validation...");

				// ==================== Method 1: Helper Way ====================
				log.info("[Method 1] Using ToolContextHelper to access metadata...");

				// Get Config using Helper
				Optional<RunnableConfig> configViaHelper = ToolContextHelper.getConfig(toolContext);
				result.helperWayWorks = configViaHelper.isPresent();

				if (configViaHelper.isPresent()) {
					RunnableConfig config = configViaHelper.get();
					Optional<Object> customData = config.metadata("test_thread_id");
					result.helperConfigValue = customData.map(Object::toString).orElse("null");
					log.info("[Method 1] ✅ Helper way succeeded - thread_id: {}", result.helperConfigValue);
				}
				else {
					log.error("[Method 1] ❌ Helper way failed - Config not found");
				}

				// Get custom metadata using Helper (type-safe)
				Optional<String> customValueViaHelper = ToolContextHelper.getMetadata(
						toolContext, "custom_test_key", String.class);
				if (customValueViaHelper.isPresent()) {
					log.info("[Method 1] ✅ Helper way got custom key: {}", customValueViaHelper.get());
				}

				// ==================== Method 2: Direct Key Access ====================
				log.info("[Method 2] Using direct key access to get metadata...");

				// Get raw context map
				Map<String, Object> context = toolContext.getContext();

				// Get Config using direct key (internal constant)
				Object configObj = context.get(AGENT_CONFIG_CONTEXT_KEY);
				result.directWayWorks = configObj != null;

				if (configObj instanceof RunnableConfig) {
					RunnableConfig config = (RunnableConfig) configObj;
					Optional<Object> customData = config.metadata("test_thread_id");
					result.directConfigValue = customData.map(Object::toString).orElse("null");
					log.info("[Method 2] ✅ Direct key way succeeded - thread_id: {}", result.directConfigValue);
				}
				else {
					log.error("[Method 2] ❌ Direct key way failed - Config not found or wrong type: {}",
							configObj != null ? configObj.getClass().getName() : "null");
				}

				// Get custom key directly
				Object customValueDirect = context.get("custom_test_key");
				if (customValueDirect != null) {
					log.info("[Method 2] ✅ Direct key way got custom key: {}", customValueDirect);
				}

				// ==================== Compare Results ====================
				if (result.helperWayWorks && result.directWayWorks) {
					result.dataConsistent = Objects.equals(result.helperConfigValue, result.directConfigValue);
					if (result.dataConsistent) {
						log.info("[Validation] ✅ Both methods returned consistent data: {}", result.helperConfigValue);
					}
					else {
						log.error("[Validation] ❌ Data mismatch! Helper={}, Direct={}",
								result.helperConfigValue, result.directConfigValue);
						result.errorMessage = "Data mismatch between helper and direct access";
					}
				}
				else {
					String error = String.format("Retrieval failed: helperWayWorks=%s, directWayWorks=%s",
							result.helperWayWorks, result.directWayWorks);
					log.error("[Validation] ❌ {}", error);
					result.errorMessage = error;
				}

				// ==================== Store Result ====================
				// Key: Tool passes result back to test code via static holder
				LAST_RESULT_HOLDER.set(result);
				log.info("[Tool Execution] ✅ Validation result stored in static holder");

			}
			catch (Exception e) {
				log.error("[Tool Execution] ❌ Exception", e);
				result.errorMessage = "Exception: " + e.getMessage();

				// Store error result even on failure
				LAST_RESULT_HOLDER.set(result);
			}

			return new Response(result);
		}

		public record Request(@JsonProperty(required = true,
				value = "validate") @JsonPropertyDescription("Set to true to run validation") boolean validate) {
		}

		public record Response(ValidationResult result) {
		}

		public static ToolCallback createToolCallback() {
			return FunctionToolCallback.builder("validate_metadata_access", new MetadataValidationTool())
				.description("Validates that ToolContextHelper and direct key access work correctly")
				.inputType(Request.class)
				.build();
		}

	}

	/**
	 * Create validation agent
	 */
	public static ReactAgent createValidationAgent(ChatModel chatModel) throws GraphRunnerException {
		ToolCallback validationTool = MetadataValidationTool.createToolCallback();

		ReactAgent agent = ReactAgent.builder()
			.name("metadata_validation_agent")
			.model(chatModel)
			.tools(validationTool)
			.systemPrompt("""
				You are a validation agent. Your task is to call the validate_metadata_access tool
				to verify that metadata can be accessed correctly via ToolContextHelper.

				When the user asks you to validate, call the tool with validate=true.
				The tool will store the validation result for subsequent checks.
				""")
			.saver(new MemorySaver())
			.build();

		return agent;
	}

	/**
	 * Run validation test in real scenario
	 *
	 * <p>Test flow:</p>
	 * <ol>
	 *   <li>Create Config with custom metadata</li>
	 *   <li>Invoke Agent to call tool (real LLM call)</li>
	 *   <li>Tool retrieves metadata using both approaches</li>
	 *   <li>Tool stores result in static holder</li>
	 *   <li>Test code retrieves and validates the result</li>
	 * </ol>
	 */
	public static ValidationResult runValidationTest() throws GraphRunnerException {
		log.info("========================================");
		log.info("ToolContextHelper Example - Validation Test");
		log.info("Real scenario: Tool execution → Store result → Validate");
		log.info("========================================\n");

		// Create ChatModel using DashScope
		String apiKey = System.getenv("AI_DASHSCOPE_API_KEY");
		if (apiKey == null || apiKey.isEmpty()) {
			throw new IllegalStateException("Please set environment variable AI_DASHSCOPE_API_KEY");
		}

		DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(apiKey).build();
		ChatModel chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();

		// Create validation Agent
		ReactAgent agent = createValidationAgent(chatModel);

		// Create Config with custom metadata
		String testThreadId = "test-" + UUID.randomUUID().toString();
		RunnableConfig config = RunnableConfig.builder()
			.threadId(testThreadId)
			.addMetadata("test_thread_id", testThreadId)
			.addMetadata("custom_test_key", "custom-value-123")
			.build();

		log.info("✅ Config created: thread_id = {}", testThreadId);
		log.info("✅ Custom metadata: test_thread_id={}, custom_test_key=custom-value-123\n",
				testThreadId);

		// Invoke Agent to trigger tool execution (real LLM call)
		String prompt = String.format(
				"Please call validate_metadata_access tool with validate=true to verify metadata access."
						+ "Expected thread_id is: %s",
				testThreadId);

		log.info("🤖 Invoking Agent, waiting for tool execution...");
		AssistantMessage response = agent.call(prompt, config);

		log.info("✅ Agent response: {}", response.getText());

		// ==================== Retrieve result from static holder ====================
		log.info("\n🔍 Reading tool validation result...");

		// After tool execution, result is stored in MetadataValidationTool's static holder
		ValidationResult result = MetadataValidationTool.getLastResult();

		if (result != null) {
			log.info("✅ Successfully retrieved tool validation result");
		}
		else {
			log.error("❌ Failed to get validation result (tool may not have executed)");
		}

		// ==================== Validate result ====================
		log.info("\n========================================");
		if (result != null) {
			log.info("Validation result: {}", result);

			// Validate result correctness
			boolean success = result.helperWayWorks
					&& result.directWayWorks
					&& result.dataConsistent
					&& testThreadId.equals(result.helperConfigValue)
					&& testThreadId.equals(result.directConfigValue);

			if (success) {
				log.info("✅ All validations passed!");
				log.info("   - Helper way works: {}", result.helperWayWorks);
				log.info("   - Direct key way works: {}", result.directWayWorks);
				log.info("   - Data consistent: {}", result.dataConsistent);
				log.info("   - Value correct: {}", result.helperConfigValue);
			}
			else {
				log.error("❌ Validation failed!");
				log.error("   - Helper way: {}", result.helperWayWorks);
				log.error("   - Direct key way: {}", result.directWayWorks);
				log.error("   - Data consistent: {}", result.dataConsistent);
				log.error("   - Expected: {}", testThreadId);
				log.error("   - Helper value: {}", result.helperConfigValue);
				log.error("   - Direct key value: {}", result.directConfigValue);
				if (result.errorMessage != null) {
					log.error("   - Error: {}", result.errorMessage);
				}
			}
		}
		else {
			log.error("❌ Cannot get validation result (tool may not have executed or failed)");
		}
		log.info("========================================");

		return result;
	}

	/**
	 * Main method
	 */
	public static void main(String[] args) {
		try {
			ValidationResult result = runValidationTest();

			// Final assertion and exit code
			if (result != null
					&& result.helperWayWorks
					&& result.directWayWorks
					&& result.dataConsistent) {
				log.info("\n🎉 Test successful! ToolContextHelper works correctly.");
				System.exit(0);
			}
			else {
				log.error("\n💥 Test failed! Please check logs.");
				System.exit(1);
			}
		}
		catch (Exception e) {
			log.error("\n💥 Test execution exception", e);
			System.exit(1);
		}
	}

}

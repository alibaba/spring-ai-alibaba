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
import com.alibaba.cloud.ai.graph.agent.tools.ToolContextHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for Issue #4452 using a real LLM (DashScope).
 * Requires env var: AI_DASHSCOPE_API_KEY
 */
@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
@DisplayName("Issue #4452 – Integration test with real LLM (DashScope)")
class Issue4452RealLlmIntegrationTest {

	private static final String USER_ID_VALUE = "user-42";

	private static final String TENANT_ID_VALUE = "tenant-acme";

	private ChatModel chatModel;

	@BeforeEach
	void setUp() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();
		this.chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();
	}

	@Test
	@DisplayName("sub-agent reads parent metadata from RunnableConfig via real LLM calls")
	void subAgent_readsParentMetadataWithRealLlm() throws Exception {

		AtomicReference<String> capturedUserId = new AtomicReference<>();
		AtomicReference<String> capturedTenantId = new AtomicReference<>();

		BiFunction<String, ToolContext, String> readMetadataFn = (input, toolCtx) -> {
			Optional<RunnableConfig> configOpt = ToolContextHelper.getConfig(toolCtx);
			if (configOpt.isEmpty()) {
				return "ERROR: no RunnableConfig found in ToolContext – Bug #4452 is not fixed!";
			}
			RunnableConfig config = configOpt.get();
			String userId = (String) config.metadata("userId").orElse("MISSING");
			String tenantId = (String) config.metadata("tenantId").orElse("MISSING");
			capturedUserId.set(userId);
			capturedTenantId.set(tenantId);
			return "userId=" + userId + ", tenantId=" + tenantId;
		};

		ToolCallback readMetadataTool = FunctionToolCallback.builder("read_metadata_tool", readMetadataFn)
				.description("读取当前运行配置中的业务元数据（userId 和 tenantId），以字符串形式返回。"
						+ "调用时 input 参数直接传空字符串即可。")
				.inputType(String.class)
				.build();

		ReactAgent metadataReporterAgent = ReactAgent.builder()
				.name("metadata_reporter")
				.model(chatModel)
				.description("专门用于读取并上报运行时配置中的业务元数据（userId、tenantId）的子 Agent。")
				.instruction("你的任务是调用 read_metadata_tool 工具，获取当前配置中的 userId 和 tenantId，"
						+ "然后将获得的信息原样返回给调用方。工具调用时 input 参数传空字符串。")
				.tools(List.of(readMetadataTool))
				.build();

		ReactAgent parentAgent = ReactAgent.builder()
				.name("supervisor_agent")
				.model(chatModel)
				.description("监督者 Agent，通过调用 metadata_reporter 子 Agent 来获取当前用户的元数据。")
				.instruction("你是一个监督者。用户需要查询当前的 userId 和 tenantId。"
						+ "请调用 metadata_reporter 工具来完成这个任务，然后把结果返回给用户。")
				.tools(List.of(AgentTool.getFunctionToolCallback(metadataReporterAgent)))
				.build();

		RunnableConfig config = RunnableConfig.builder()
				.threadId("integration-test-thread-001")
				.addMetadata("userId", USER_ID_VALUE)
				.addMetadata("tenantId", TENANT_ID_VALUE)
				.build();

		System.out.println("[Test] Invoking parent agent with real LLM...");

		Optional<OverAllState> result = parentAgent.getAndCompileGraph()
				.invoke(java.util.Map.of("messages", List.of(new UserMessage("请帮我查询当前的 userId 和 tenantId。"))),
						config);

		assertTrue(result.isPresent(), "Parent agent must produce a result");

		System.out.println("[Test] Captured userId   : " + capturedUserId.get());
		System.out.println("[Test] Captured tenantId : " + capturedTenantId.get());

		Optional<List> messages = result.get().value("messages", List.class);
		if (messages.isPresent()) {
			@SuppressWarnings("unchecked")
			List<Message> msgList = (List<Message>) messages.get();
			if (!msgList.isEmpty() && msgList.get(msgList.size() - 1) instanceof AssistantMessage lastMsg) {
				System.out.println("[Test] Final answer: " + lastMsg.getText());
			}
		}

		assertNotNull(capturedUserId.get(),
				"[BUG #4452] read_metadata_tool was never called – sub-agent did not receive RunnableConfig from parent.");
		assertEquals(USER_ID_VALUE, capturedUserId.get(),
				"userId in sub-agent's config must match parent's metadata");
		assertEquals(TENANT_ID_VALUE, capturedTenantId.get(),
				"tenantId in sub-agent's config must match parent's metadata");
	}
}

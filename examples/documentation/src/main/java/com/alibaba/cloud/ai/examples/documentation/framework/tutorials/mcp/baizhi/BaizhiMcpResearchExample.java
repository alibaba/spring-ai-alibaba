/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.examples.documentation.framework.tutorials.mcp.baizhi;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

/**
 * Optional, self-contained research example built on the existing MCP tutorial.
 *
 * <p>
 * The scenario is limited on purpose: search for public information, read a relevant page,
 * and answer from the returned tool content. Only three explicitly named tools are
 * registered, the service credential is separate from the model credential, and missing
 * configuration fails before the example reaches the network.
 *
 * <p>
 * The example does not add an MCP transport implementation, does not add a default service
 * to {@code application.yml}, and does not change the existing travel example.
 *
 * <p>
 * It is invoked on demand (see {@code BaizhiMcpResearchExampleController}) so a missing
 * configuration does not break application startup.
 *
 * @author Spring AI Alibaba
 */
@Service
public class BaizhiMcpResearchExample {

	private static final Logger log = LoggerFactory.getLogger(BaizhiMcpResearchExample.class);

	private static final String MODEL_API_KEY_ENV = "AI_DASHSCOPE_API_KEY";

	private static final String INSTRUCTION = """
			You are a careful public-information research assistant.
			Use only the tools provided to you. First search for sources, then read at least one
			useful page before answering. Base the answer on the returned tool content and cite
			the URLs you actually read. If a required tool is unavailable or a call fails, say so
			instead of guessing. Never put credentials in tool arguments.
			""";

	private static final String QUESTION = """
			Research the public information about the Model Context Protocol (MCP):
			what problem does it solve, and how do client and server communicate?
			Search for sources, read at least one relevant page, and answer with the URLs used.
			""";

	/**
	 * Run the limited-tool research scenario.
	 * @throws GraphRunnerException when the agent run fails
	 * @throws IllegalStateException when configuration is missing or the MCP session
	 * cannot be established
	 */
	public void runLimitedToolResearchExample() throws GraphRunnerException {
		// Validate the service credential first: a missing key fails before any network
		// request, and the model credential stays separate.
		BaizhiMcpResearchConfig config = BaizhiMcpResearchConfig.fromEnvironment();
		ChatModel chatModel = createChatModel();

		try (BaizhiMcpResearchClient researchClient = BaizhiMcpResearchClient.connect(config)) {
			log.info("Registered tools: {}", researchClient.selectedToolNames());
			ReactAgent agent = ReactAgent.builder()
				.name("baizhi_public_info_researcher")
				.description("Public-information research assistant with three explicitly selected MCP tools")
				.model(chatModel)
				.instruction(INSTRUCTION)
				.tools(researchClient.toolCallbacks())
				.saver(new MemorySaver())
				.build();

			AssistantMessage answer = agent.call(QUESTION);
			log.info("Research answer:\n{}", answer.getText());
		}
	}

	private static ChatModel createChatModel() {
		String apiKey = System.getenv(MODEL_API_KEY_ENV);
		if (apiKey == null || apiKey.isBlank()) {
			throw new IllegalStateException(MODEL_API_KEY_ENV
					+ " is not set. The model credential is separate from " + BaizhiMcpResearchConfig.API_KEY_ENV);
		}
		DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(apiKey).build();
		return DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();
	}

}

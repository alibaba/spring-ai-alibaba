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

package com.alibaba.cloud.ai.studio.core.agent;

import com.alibaba.cloud.ai.studio.core.base.manager.AppComponentManager;
import com.alibaba.cloud.ai.studio.core.base.manager.DocumentRetrieverManager;
import com.alibaba.cloud.ai.studio.core.base.manager.FileManager;
import com.alibaba.cloud.ai.studio.core.base.service.McpServerService;
import com.alibaba.cloud.ai.studio.core.base.service.PluginService;
import com.alibaba.cloud.ai.studio.core.base.service.ToolExecutionService;
import com.alibaba.cloud.ai.studio.core.config.CommonConfig;
import com.alibaba.cloud.ai.studio.core.model.llm.ModelFactory;
import com.alibaba.cloud.ai.studio.runtime.domain.agent.AgentRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.agent.AgentResponse;
import com.alibaba.cloud.ai.studio.runtime.domain.app.AgentConfig;
import com.alibaba.cloud.ai.studio.runtime.domain.chat.ChatMessage;
import com.alibaba.cloud.ai.studio.runtime.domain.chat.ContentType;
import com.alibaba.cloud.ai.studio.runtime.domain.chat.MessageRole;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BasicAgentExecutorTest {

	@Test
	void shouldNotInitializeSpringAiToolCallingManagerWhenNoToolsAreConfiguredAndModelReturnsNoToolCalls() {
		CapturingChatModel chatModel = new CapturingChatModel();
		ModelFactory modelFactory = mock(ModelFactory.class);
		when(modelFactory.getChatModel(any())).thenReturn(chatModel);

		ToolExecutionService toolExecutionService = mock(ToolExecutionService.class);
		PluginService pluginService = mock(PluginService.class);
		McpServerService mcpServerService = mock(McpServerService.class);
		AppComponentManager appComponentManager = mock(AppComponentManager.class);

		BasicAgentExecutor executor = new BasicAgentExecutor(toolExecutionService, pluginService, mcpServerService,
				appComponentManager, mock(DocumentRetrieverManager.class), mock(ChatMemory.class), mock(CommonConfig.class),
				modelFactory, mock(FileManager.class));

		AgentRequest request = new AgentRequest();
		request.setMessages(
				List.of(ChatMessage.builder().role(MessageRole.USER).contentType(ContentType.TEXT).content("hello").build()));

		AgentConfig config = new AgentConfig();
		config.setModel("qwen-plus");

		AgentContext context = new AgentContext();
		context.setConfig(config);
		context.setRequest(request);

		try (MockedStatic<ToolCallingManager> toolCallingManager = mockStatic(ToolCallingManager.class)) {
			toolCallingManager.when(ToolCallingManager::builder)
				.thenThrow(new AssertionError("ToolCallingManager must not be initialized when no tools are configured"));

			AgentResponse response = executor.execute(context, request);

			assertEquals("ok", response.getMessage().getContent());
			verifyNoInteractions(pluginService, toolExecutionService, mcpServerService, appComponentManager);
		}
	}

	private static final class CapturingChatModel implements ChatModel {

		@Override
		public ChatResponse call(Prompt prompt) {
			return new ChatResponse(
					List.of(new Generation(new AssistantMessage("ok"),
							ChatGenerationMetadata.builder().finishReason("stop").build())),
					ChatResponseMetadata.builder().model("qwen-plus").usage(new DefaultUsage(0, 0, 0)).build());
		}

	}

}

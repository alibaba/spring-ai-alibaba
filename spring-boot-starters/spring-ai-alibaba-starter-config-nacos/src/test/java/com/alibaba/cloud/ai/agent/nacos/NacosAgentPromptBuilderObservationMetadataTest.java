/*
 * Copyright 2026-2027 the original author or authors.
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
package com.alibaba.cloud.ai.agent.nacos;

import java.lang.reflect.Field;
import java.util.Map;

import com.alibaba.cloud.ai.agent.nacos.vo.PromptVO;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.node.AgentLlmNode;
import com.alibaba.cloud.ai.observation.model.ObservationMetadataAwareOptions;
import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.client.config.NacosConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NacosAgentPromptBuilderObservationMetadataTest {

	@Mock
	private NacosConfigService nacosConfigService;

	private NacosOptions nacosOptions;

	@BeforeEach
	void setUp() throws Exception {
		this.nacosOptions = mock(NacosOptions.class);
		when(this.nacosOptions.getNacosConfigService()).thenReturn(this.nacosConfigService);
		when(this.nacosOptions.getPromptKey()).thenReturn("test-prompt");
		setupPromptConfig();
	}

	@Test
	void promptMetadataDoesNotOverrideNonOpenAiModelDefaults() throws Exception {
		ChatModel chatModel = new TestChatModel(DashScopeChatOptions.builder()
				.model("qwen-plus")
				.temperature(0.2)
				.build());

		ReactAgent agent = new NacosAgentPromptBuilder()
				.nacosOptions(this.nacosOptions)
				.name("test-agent")
				.model(chatModel)
				.build();

		ChatOptions effectiveOptions = getChatOptions(agent);
		DashScopeChatOptions dashScopeOptions = assertInstanceOf(DashScopeChatOptions.class, effectiveOptions);
		ObservationMetadataAwareOptions observationOptions = assertInstanceOf(ObservationMetadataAwareOptions.class,
				effectiveOptions);
		assertEquals("qwen-plus", dashScopeOptions.getModel());
		assertEquals(0.2, dashScopeOptions.getTemperature());
		assertEquals(Map.of("promptKey", "test-prompt", "promptVersion", "v1"),
				observationOptions.getObservationMetadata());
	}

	@Test
	void explicitDashScopeOptionsCanCarryPromptMetadata() throws Exception {
		ChatModel chatModel = new TestChatModel(DashScopeChatOptions.builder()
				.model("qwen-plus")
				.temperature(0.2)
				.build());

		ReactAgent agent = new NacosAgentPromptBuilder()
				.nacosOptions(this.nacosOptions)
				.name("test-agent")
				.model(chatModel)
				.chatOptions(DashScopeChatOptions.builder().temperature(0.7).enableThinking(true).build())
				.build();

		ChatOptions effectiveOptions = getChatOptions(agent);
		DashScopeChatOptions dashScopeOptions = assertInstanceOf(DashScopeChatOptions.class, effectiveOptions);
		ObservationMetadataAwareOptions observationOptions = assertInstanceOf(ObservationMetadataAwareOptions.class,
				effectiveOptions);
		assertEquals("qwen-plus", dashScopeOptions.getModel());
		assertEquals(0.7, dashScopeOptions.getTemperature());
		assertEquals(Boolean.TRUE, dashScopeOptions.getEnableThinking());
		assertEquals(Map.of("promptKey", "test-prompt", "promptVersion", "v1"),
				observationOptions.getObservationMetadata());
	}

	@SuppressWarnings("deprecation")
	@Test
	void chatClientDefaultsKeepBackingModelAndPromptMetadata() throws Exception {
		ChatModel chatModel = new TestChatModel(DashScopeChatOptions.builder()
				.model("qwen-plus")
				.temperature(0.2)
				.build());
		ChatClient chatClient = ChatClient.builder(chatModel)
				.defaultOptions(DashScopeChatOptions.builder().temperature(0.7))
				.build();

		ReactAgent agent = new NacosAgentPromptBuilder()
				.nacosOptions(this.nacosOptions)
				.name("test-agent")
				.chatClient(chatClient)
				.build();

		ChatOptions requestOptions = buildRequestOptions(getChatClient(agent));
		DashScopeChatOptions dashScopeOptions = assertInstanceOf(DashScopeChatOptions.class, requestOptions);
		ObservationMetadataAwareOptions observationOptions = assertInstanceOf(ObservationMetadataAwareOptions.class,
				requestOptions);
		assertEquals("qwen-plus", dashScopeOptions.getModel());
		assertEquals(0.7, dashScopeOptions.getTemperature());
		assertEquals(Map.of("promptKey", "test-prompt", "promptVersion", "v1"),
				observationOptions.getObservationMetadata());
	}

	@Test
	void promptListenerRefreshesChatClientRequestObservationMetadata() throws Exception {
		ChatModel chatModel = new TestChatModel(DashScopeChatOptions.builder()
				.model("qwen-plus")
				.temperature(0.2)
				.build());

		ReactAgent agent = new NacosAgentPromptBuilder()
				.nacosOptions(this.nacosOptions)
				.name("test-agent")
				.model(chatModel)
				.build();

		var listenerCaptor = forClass(Listener.class);
		verify(this.nacosConfigService).addListener(eq("prompt-test-prompt.json"), eq("nacos-ai-meta"),
				listenerCaptor.capture());
		PromptVO refreshedPromptVO = new PromptVO();
		refreshedPromptVO.setPromptKey("test-prompt");
		refreshedPromptVO.setVersion("v2");
		refreshedPromptVO.setTemplate("You are a refreshed test assistant");
		listenerCaptor.getValue().receiveConfigInfo(JSON.toJSONString(refreshedPromptVO));

		ChatOptions requestOptions = buildRequestOptions(getChatClient(agent));
		ObservationMetadataAwareOptions observationOptions = assertInstanceOf(ObservationMetadataAwareOptions.class,
				requestOptions);
		assertEquals(Map.of("promptKey", "test-prompt", "promptVersion", "v2"),
				observationOptions.getObservationMetadata());
	}

	private void setupPromptConfig() throws NacosException {
		PromptVO promptVO = new PromptVO();
		promptVO.setPromptKey("test-prompt");
		promptVO.setVersion("v1");
		promptVO.setTemplate("You are a test assistant");
		when(this.nacosConfigService.getConfig(eq("prompt-test-prompt.json"), anyString(), anyLong()))
				.thenReturn(JSON.toJSONString(promptVO));
	}

	private static ChatOptions getChatOptions(ReactAgent agent) throws Exception {
		Field llmNodeField = ReactAgent.class.getDeclaredField("llmNode");
		llmNodeField.setAccessible(true);
		AgentLlmNode llmNode = (AgentLlmNode) llmNodeField.get(agent);

		Field chatOptionsField = AgentLlmNode.class.getDeclaredField("chatOptions");
		chatOptionsField.setAccessible(true);
		return (ChatOptions) chatOptionsField.get(llmNode);
	}

	private static ChatClient getChatClient(ReactAgent agent) throws Exception {
		Field llmNodeField = ReactAgent.class.getDeclaredField("llmNode");
		llmNodeField.setAccessible(true);
		AgentLlmNode llmNode = (AgentLlmNode) llmNodeField.get(agent);

		Field chatClientField = AgentLlmNode.class.getDeclaredField("chatClient");
		chatClientField.setAccessible(true);
		return (ChatClient) chatClientField.get(llmNode);
	}

	private static ChatOptions buildRequestOptions(ChatClient chatClient) throws Exception {
		Field defaultRequestField = chatClient.getClass().getDeclaredField("defaultChatClientRequest");
		defaultRequestField.setAccessible(true);
		Object defaultChatClientRequest = defaultRequestField.get(chatClient);

		ChatModel chatModel = getField(defaultChatClientRequest, "chatModel", ChatModel.class);
		ChatOptions.Builder<?> builder = chatModel.getOptions().mutate();
		ChatOptions.Builder<?> optionsCustomizer = getField(defaultChatClientRequest, "optionsCustomizer",
				ChatOptions.Builder.class);
		if (optionsCustomizer != null) {
			builder.combineWith(optionsCustomizer);
		}
		return builder.build();
	}

	@SuppressWarnings("unchecked")
	private static <T> T getField(Object target, String fieldName, Class<?> fieldType) throws Exception {
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		Object value = field.get(target);
		if (value == null || fieldType.isInstance(value)) {
			return (T) value;
		}
		throw new IllegalStateException("Unexpected field type: " + fieldName);
	}

	private static final class TestChatModel implements ChatModel {

		private final ChatOptions options;

		private TestChatModel(ChatOptions options) {
			this.options = options;
		}

		@Override
		public ChatOptions getOptions() {
			return this.options;
		}

		@Override
		public ChatResponse call(Prompt prompt) {
			throw new UnsupportedOperationException("This test only verifies prompt metadata options.");
		}

	}

}

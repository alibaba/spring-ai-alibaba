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
import java.util.Collections;
import java.util.Map;

import com.alibaba.cloud.ai.agent.nacos.vo.AgentVO;
import com.alibaba.cloud.ai.agent.nacos.vo.McpServersVO;
import com.alibaba.cloud.ai.agent.nacos.vo.ModelVO;
import com.alibaba.cloud.ai.agent.nacos.vo.PromptVO;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.node.AgentLlmNode;
import com.alibaba.cloud.ai.observation.model.ObservationMetadataAwareOptions;
import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.client.config.NacosConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NacosReactAgentBuilderObservationMetadataTest {

	@Mock
	private NacosConfigService nacosConfigService;

	private NacosOptions nacosOptions;

	@BeforeEach
	void setUp() throws Exception {
		this.nacosOptions = mock(NacosOptions.class);
		when(this.nacosOptions.getNacosConfigService()).thenReturn(this.nacosConfigService);
		when(this.nacosOptions.getAgentName()).thenReturn("test-agent");
		setupNacosConfigs();
	}

	@Test
	void buildCreatesObservationMetadataOptionsThatSurviveMutation() {
		NacosReactAgentBuilder builder = new NacosReactAgentBuilder();
		builder.nacosOptions(this.nacosOptions)
				.name("test-agent")
				.build();

		ObservationMetadataAwareOptions holderOptions = builder.agentVOHolder.getObservationMetadataAwareOptions();
		assertEquals(Map.of("promptKey", "test-prompt", "promptVersion", "v1"),
				holderOptions.getObservationMetadata());

		OpenAiChatOptions requestOptions = ((OpenAiChatOptions) holderOptions).mutate()
				.temperature(0.4)
				.build();

		ObservationMetadataAwareOptions requestObservationOptions = assertInstanceOf(
				ObservationMetadataAwareOptions.class, requestOptions);
		assertEquals("gpt-4", requestOptions.getModel());
		assertEquals(0.4, requestOptions.getTemperature());
		assertEquals(Map.of("promptKey", "test-prompt", "promptVersion", "v1"),
				requestObservationOptions.getObservationMetadata());
	}

	@Test
	void refreshedPromptMetadataIsVisibleInChatClientRequestOptions() throws Exception {
		NacosReactAgentBuilder builder = new NacosReactAgentBuilder();
		ReactAgent agent = builder.nacosOptions(this.nacosOptions)
				.name("test-agent")
				.build();
		ObservationMetadataAwareOptions holderOptions = builder.agentVOHolder.getObservationMetadataAwareOptions();
		ChatClient chatClient = getChatClient(agent);

		holderOptions.getObservationMetadata().put("promptVersion", "v2");
		ChatOptions requestOptions = buildRequestOptions(chatClient);

		ObservationMetadataAwareOptions requestObservationOptions = assertInstanceOf(
				ObservationMetadataAwareOptions.class, requestOptions);
		assertEquals(Map.of("promptKey", "test-prompt", "promptVersion", "v2"),
				requestObservationOptions.getObservationMetadata());
	}

	private void setupNacosConfigs() throws NacosException {
		AgentVO agentVO = new AgentVO();
		agentVO.setPromptKey("test-prompt");
		agentVO.setDescription("Test agent description");
		when(this.nacosConfigService.getConfig(eq("agent-base.json"), anyString(), anyLong()))
				.thenReturn(JSON.toJSONString(agentVO));

		PromptVO promptVO = new PromptVO();
		promptVO.setPromptKey("test-prompt");
		promptVO.setVersion("v1");
		promptVO.setTemplate("You are a test assistant");
		when(this.nacosConfigService.getConfig(eq("prompt-test-prompt.json"), anyString(), anyLong()))
				.thenReturn(JSON.toJSONString(promptVO));

		ModelVO modelVO = new ModelVO();
		modelVO.setBaseUrl("https://api.openai.com/v1");
		modelVO.setApiKey("test-api-key");
		modelVO.setModel("gpt-4");
		modelVO.setTemperature("0.7");
		when(this.nacosConfigService.getConfig(eq("model.json"), anyString(), anyLong()))
				.thenReturn(JSON.toJSONString(modelVO));

		McpServersVO mcpServersVO = new McpServersVO();
		mcpServersVO.setMcpServers(Collections.emptyList());
		when(this.nacosConfigService.getConfig(eq("mcp-servers.json"), anyString(), anyLong()))
				.thenReturn(JSON.toJSONString(mcpServersVO));
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

}

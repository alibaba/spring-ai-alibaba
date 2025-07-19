/*
 * Copyright 2025 the original author or authors.
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
package com.alibaba.cloud.ai.example.manus.llm;

import com.alibaba.cloud.ai.example.manus.dynamic.model.entity.DynamicModelEntity;
import com.alibaba.cloud.ai.example.manus.event.JmanusListener;
import com.alibaba.cloud.ai.example.manus.event.ModelChangeEvent;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatProperties;
import org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingProperties;
import org.springframework.ai.model.tool.DefaultToolExecutionEligibilityPredicate;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionEligibilityPredicate;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LlmService implements ILlmService, JmanusListener<ModelChangeEvent> {

	private static final Logger log = LoggerFactory.getLogger(LlmService.class);

	private ChatClient agentExecutionClient;

	private ChatClient planningChatClient;

	private ChatClient finalizeChatClient;

	private ChatMemory conversationMemory;

	private ChatMemory agentMemory;

	private ChatModel chatModel;

	private Map<Long, ChatClient> clients = new ConcurrentHashMap<>();

	/*
	 * 创建自定义chatModel所需
	 */
	@Autowired
	private ObjectProvider<RestClient.Builder> restClientBuilderProvider;

	@Autowired
	private ObjectProvider<WebClient.Builder> webClientBuilderProvider;

	@Autowired
	private ToolCallingManager toolCallingManager;

	@Autowired
	private RetryTemplate retryTemplate;

	@Autowired
	private ResponseErrorHandler responseErrorHandler;

	@Autowired
	private ObjectProvider<ObservationRegistry> observationRegistry;

	@Autowired
	private ObjectProvider<ChatModelObservationConvention> observationConvention;

	@Autowired
	private ObjectProvider<ToolExecutionEligibilityPredicate> openAiToolExecutionEligibilityPredicate;

	public LlmService(ChatModel chatModel) {
		this.chatModel = chatModel;
	}

	@Override
	public ChatClient getAgentChatClient() {
		return agentExecutionClient;
	}

	@Override
	public ChatClient getDynamicChatClient(DynamicModelEntity model) {
		Long modelId = model.getId();
		if (clients.containsKey(modelId)) {
			return clients.get(modelId);
		}
		return buildOrUpdateDynamicChatClient(model);
	}

	public ChatClient buildOrUpdateDynamicChatClient(DynamicModelEntity model) {
		Long modelId = model.getId();
		String host = model.getBaseUrl();
		String apiKey = model.getApiKey();
		String modelName = model.getModelName();
		Map<String, String> headers = model.getHeaders();
		OpenAiApi openAiApi = OpenAiApi.builder().baseUrl(host).apiKey(apiKey).build();

		OpenAiChatOptions chatOptions = OpenAiChatOptions.builder().model(modelName).build();
		if (headers != null) {
			chatOptions.setHttpHeaders(headers);
		}
		OpenAiChatModel openAiChatModel = OpenAiChatModel.builder()
			.openAiApi(openAiApi)
			.defaultOptions(chatOptions)
			.build();
		ChatClient client = ChatClient.builder(openAiChatModel)
			// .defaultAdvisors(MessageChatMemoryAdvisor.builder(agentMemory).build())
			.defaultAdvisors(new SimpleLoggerAdvisor())
			.defaultOptions(OpenAiChatOptions.builder().internalToolExecutionEnabled(false).build())
			.build();
		clients.put(modelId, client);
		log.info("Build or update dynamic chat client for model: {}", modelName);
		return client;
	}

	@Override
	public ChatMemory getAgentMemory(Integer maxMessages) {
		if (agentMemory == null) {
			agentMemory = MessageWindowChatMemory.builder().maxMessages(maxMessages).build();
		}
		return agentMemory;
	}

	@Override
	public void clearAgentMemory(String planId) {
		this.agentMemory.clear(planId);
	}

	@Override
	public ChatClient getPlanningChatClient() {
		return planningChatClient;
	}

	@Override
	public void clearConversationMemory(String planId) {
		if (this.conversationMemory == null) {
			// Default to 100 messages if not specified elsewhere
			this.conversationMemory = MessageWindowChatMemory.builder().maxMessages(100).build();
		}
		this.conversationMemory.clear(planId);
	}

	@Override
	public ChatClient getFinalizeChatClient() {
		return finalizeChatClient;
	}

	@Override
	public ChatModel getChatModel() {
		return this.chatModel;
	}

	@Override
	public ChatMemory getConversationMemory(Integer maxMessages) {
		if (conversationMemory == null) {
			conversationMemory = MessageWindowChatMemory.builder().maxMessages(maxMessages).build();
		}
		return conversationMemory;
	}

	@Override
	public void onEvent(ModelChangeEvent event) {

		OpenAiChatOptions defaultOptions = (OpenAiChatOptions) chatModel.getDefaultOptions();
		DynamicModelEntity dynamicModelEntity = event.getDynamicModelEntity();

		if (this.planningChatClient == null) {
			// Execute and summarize planning, use the same memory
			this.planningChatClient = buildPlanningChatClient(dynamicModelEntity, defaultOptions);
		}

		if (this.agentExecutionClient == null) {
			// Each agent execution process uses independent memory
			this.agentExecutionClient = buildAgentExecutionClient(dynamicModelEntity, defaultOptions);
		}

		if (this.finalizeChatClient == null) {
			this.finalizeChatClient = buildFinalizeChatClient(dynamicModelEntity, defaultOptions);
		}

		buildOrUpdateDynamicChatClient(dynamicModelEntity);
	}

	private ChatClient buildPlanningChatClient(DynamicModelEntity dynamicModelEntity,
			OpenAiChatOptions defaultOptions) {
		OpenAiChatModel chatModel = openAiChatModel(dynamicModelEntity, defaultOptions);
		return ChatClient.builder(chatModel)
			.defaultAdvisors(new SimpleLoggerAdvisor())
			.defaultOptions(OpenAiChatOptions.fromOptions(defaultOptions))
			.build();
	}

	private ChatClient buildAgentExecutionClient(DynamicModelEntity dynamicModelEntity,
			OpenAiChatOptions defaultOptions) {
		defaultOptions.setInternalToolExecutionEnabled(false);
		OpenAiChatModel chatModel = openAiChatModel(dynamicModelEntity, defaultOptions);
		return ChatClient.builder(chatModel)
			// .defaultAdvisors(MessageChatMemoryAdvisor.builder(agentMemory).build())
			.defaultAdvisors(new SimpleLoggerAdvisor())
			.defaultOptions(OpenAiChatOptions.fromOptions(defaultOptions))
			.build();
	}

	private ChatClient buildFinalizeChatClient(DynamicModelEntity dynamicModelEntity,
			OpenAiChatOptions defaultOptions) {
		OpenAiChatModel chatModel = openAiChatModel(dynamicModelEntity, defaultOptions);
		return ChatClient.builder(chatModel)
			// .defaultAdvisors(MessageChatMemoryAdvisor.builder(conversationMemory).build())
			.defaultAdvisors(new SimpleLoggerAdvisor())
			.build();
	}

	public OpenAiChatModel openAiChatModel(DynamicModelEntity dynamicModelEntity, OpenAiChatOptions defaultOptions) {
		defaultOptions.setModel(dynamicModelEntity.getModelName());
		Map<String, String> headers = dynamicModelEntity.getHeaders();
		if (headers != null) {
			defaultOptions.setHttpHeaders(headers);
		}
		var openAiApi = openAiApi(restClientBuilderProvider.getIfAvailable(RestClient::builder),
				webClientBuilderProvider.getIfAvailable(WebClient::builder), responseErrorHandler, dynamicModelEntity);
		OpenAiChatOptions options = OpenAiChatOptions.fromOptions(defaultOptions);
		var chatModel = OpenAiChatModel.builder()
			.openAiApi(openAiApi)
			.defaultOptions(options)
			.toolCallingManager(toolCallingManager)
			.toolExecutionEligibilityPredicate(
					openAiToolExecutionEligibilityPredicate.getIfUnique(DefaultToolExecutionEligibilityPredicate::new))
			.retryTemplate(retryTemplate)
			.observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
			.build();

		observationConvention.ifAvailable(chatModel::setObservationConvention);

		return chatModel;
	}

	private OpenAiApi openAiApi(RestClient.Builder restClientBuilder, WebClient.Builder webClientBuilder,
			ResponseErrorHandler responseErrorHandler, DynamicModelEntity dynamicModelEntity) {
		Map<String, String> headers = dynamicModelEntity.getHeaders();
		MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap<>();
		if (headers != null) {
			headers.forEach((key, value) -> multiValueMap.add(key, value));
		}

		return OpenAiApi.builder()
			.baseUrl(dynamicModelEntity.getBaseUrl())
			.apiKey(new SimpleApiKey(dynamicModelEntity.getApiKey()))
			.headers(multiValueMap)
			.completionsPath(OpenAiChatProperties.DEFAULT_COMPLETIONS_PATH)
			.embeddingsPath(OpenAiEmbeddingProperties.DEFAULT_EMBEDDINGS_PATH)
			.restClientBuilder(restClientBuilder)
			.webClientBuilder(webClientBuilder)
			.responseErrorHandler(responseErrorHandler)
			.build();
	}

}

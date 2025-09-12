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
package com.alibaba.cloud.ai.manus.llm;

import com.alibaba.cloud.ai.manus.config.ManusProperties;
import com.alibaba.cloud.ai.manus.event.JmanusListener;
import com.alibaba.cloud.ai.manus.event.ModelChangeEvent;
import com.alibaba.cloud.ai.manus.model.entity.DynamicModelEntity;
import com.alibaba.cloud.ai.manus.model.repository.DynamicModelRepository;

import io.micrometer.observation.ObservationRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.model.tool.DefaultToolExecutionEligibilityPredicate;
import org.springframework.ai.model.tool.ToolExecutionEligibilityPredicate;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
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

	private Map<Long, ChatClient> clients = new ConcurrentHashMap<>();

	/*
	 * Required for creating custom chatModel
	 */
	@Autowired
	private ObjectProvider<RestClient.Builder> restClientBuilderProvider;

	@Autowired
	private ObjectProvider<WebClient.Builder> webClientBuilderProvider;

	@Autowired
	private ObjectProvider<ObservationRegistry> observationRegistry;

	@Autowired
	private ObjectProvider<ChatModelObservationConvention> observationConvention;

	@Autowired
	private ObjectProvider<ToolExecutionEligibilityPredicate> openAiToolExecutionEligibilityPredicate;

	@Autowired(required = false)
	private ManusProperties manusProperties;

	@Autowired
	private DynamicModelRepository dynamicModelRepository;

	@Autowired
	private ChatMemoryRepository chatMemoryRepository;

	@Autowired
	private LlmTraceRecorder llmTraceRecorder;

	public LlmService() {
	}

	@PostConstruct
	public void initializeChatClients() {
		try {
			log.info("Checking and init ChatClient instance...");

			DynamicModelEntity defaultModel = dynamicModelRepository.findByIsDefaultTrue();
			if (defaultModel == null) {
				List<DynamicModelEntity> availableModels = dynamicModelRepository.findAll();
				if (!availableModels.isEmpty()) {
					defaultModel = availableModels.get(0);
					log.info("Cannot find default model, use the first one: {}", defaultModel.getModelName());
				}
			}
			else {
				log.info("Find default model: {}", defaultModel.getModelName());
			}

			if (defaultModel != null) {
				initializeChatClientsWithModel(defaultModel);
				log.info("ChatClient init success");
			}
			else {
				log.warn("Cannot find any model，ChatClient will be initialize after model being configured");
			}
		}
		catch (Exception e) {
			log.error("Init ChatClient failed", e);
		}
	}

	private void initializeChatClientsWithModel(DynamicModelEntity model) {
		OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder();

		if (model.getTemperature() != null) {
			optionsBuilder.temperature(model.getTemperature());
		}

		if (model.getTopP() != null) {
			optionsBuilder.topP(model.getTopP());
		}

		OpenAiChatOptions defaultOptions = optionsBuilder.build();

		if (this.planningChatClient == null) {
			this.planningChatClient = buildPlanningChatClient(model, defaultOptions);
			log.debug("Planning ChatClient init finish");
		}

		// Initialize agentExecutionClient
		if (this.agentExecutionClient == null) {
			this.agentExecutionClient = buildAgentExecutionClient(model, defaultOptions);
			log.debug("Agent Execution Client init finish");
		}

		// Initialize finalizeChatClient
		if (this.finalizeChatClient == null) {
			this.finalizeChatClient = buildFinalizeChatClient(model, defaultOptions);
			log.debug("Finalize ChatClient init finish");
		}

		// Ensure dynamic ChatClient is also created
		buildOrUpdateDynamicChatClient(model);
	}

	private void tryLazyInitialization() {
		try {
			DynamicModelEntity defaultModel = dynamicModelRepository.findByIsDefaultTrue();
			if (defaultModel == null) {
				List<DynamicModelEntity> availableModels = dynamicModelRepository.findAll();
				if (!availableModels.isEmpty()) {
					defaultModel = availableModels.get(0);
				}
			}

			if (defaultModel != null) {
				log.info("Lazy init ChatClient, using model: {}", defaultModel.getModelName());
				initializeChatClientsWithModel(defaultModel);
			}
		}
		catch (Exception e) {
			log.error("Lazy init ChatClient failed", e);
		}
	}

	@Override
	public ChatClient getAgentChatClient() {
		if (agentExecutionClient == null) {
			log.warn("Agent ChatClient not initialized...");
			tryLazyInitialization();

			if (agentExecutionClient == null) {
				throw new IllegalStateException("Agent ChatClient not initialized, please specify model first");
			}
		}
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

		OpenAiChatOptions.Builder chatOptionsBuilder = OpenAiChatOptions.builder().model(modelName);

		if (model.getTemperature() != null) {
			chatOptionsBuilder.temperature(model.getTemperature());
		}

		if (model.getTopP() != null) {
			chatOptionsBuilder.topP(model.getTopP());
		}

		chatOptionsBuilder.internalToolExecutionEnabled(false);

		OpenAiChatOptions chatOptions = chatOptionsBuilder.build();
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
			.build();
		clients.put(modelId, client);
		log.info("Build or update dynamic chat client for model: {}", modelName);
		return client;
	}

	@Override
	public ChatMemory getAgentMemory(Integer maxMessages) {
		if (agentMemory == null) {
			agentMemory = MessageWindowChatMemory.builder()
				// in memory use by agent
				.chatMemoryRepository(new InMemoryChatMemoryRepository())
				.maxMessages(maxMessages)
				.build();
		}
		return agentMemory;
	}

	@Override
	public void clearAgentMemory(String memoryId) {
		if (this.agentMemory != null) {
			this.agentMemory.clear(memoryId);
		}
	}

	@Override
	public ChatClient getPlanningChatClient() {
		if (planningChatClient == null) {
			// Try lazy initialization
			log.warn("Agent ChatClient not initialized...");
			tryLazyInitialization();

			if (planningChatClient == null) {
				throw new IllegalStateException("Agent ChatClient not initialized, please specify model first");
			}
		}
		return planningChatClient;
	}

	@Override
	public void clearConversationMemory(String memoryId) {
		if (this.conversationMemory == null) {
			// Default to 100 messages if not specified elsewhere
			this.conversationMemory = MessageWindowChatMemory.builder()
				.chatMemoryRepository(chatMemoryRepository)
				.maxMessages(100)
				.build();
		}
		this.conversationMemory.clear(memoryId);
	}

	@Override
	public ChatClient getFinalizeChatClient() {
		if (finalizeChatClient == null) {
			// Try lazy initialization
			log.warn("Agent ChatClient not initialized...");
			tryLazyInitialization();

			if (finalizeChatClient == null) {
				throw new IllegalStateException("Agent ChatClient not initialized, please specify model first");
			}
		}
		return finalizeChatClient;
	}

	@Override
	public ChatMemory getConversationMemory(Integer maxMessages) {
		if (conversationMemory == null) {
			conversationMemory = MessageWindowChatMemory.builder()
				.chatMemoryRepository(chatMemoryRepository)
				.maxMessages(maxMessages)
				.build();
		}
		return conversationMemory;
	}

	@Override
	public void onEvent(ModelChangeEvent event) {
		DynamicModelEntity dynamicModelEntity = event.getDynamicModelEntity();

		initializeChatClientsWithModel(dynamicModelEntity);

		if (dynamicModelEntity.getIsDefault()) {
			log.info("Model updated");
			this.planningChatClient = null;
			this.agentExecutionClient = null;
			this.finalizeChatClient = null;
			initializeChatClientsWithModel(dynamicModelEntity);
		}
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
		if (defaultOptions.getTemperature() == null && dynamicModelEntity.getTemperature() != null) {
			defaultOptions.setTemperature(dynamicModelEntity.getTemperature());
		}
		if (defaultOptions.getTopP() == null && dynamicModelEntity.getTopP() != null) {
			defaultOptions.setTopP(dynamicModelEntity.getTopP());
		}
		Map<String, String> headers = dynamicModelEntity.getHeaders();
		if (headers == null) {
			headers = new HashMap<>();
		}
		headers.put("User-Agent", "JManus/3.0.2-SNAPSHOT");
		defaultOptions.setHttpHeaders(headers);
		var openAiApi = openAiApi(restClientBuilderProvider.getIfAvailable(RestClient::builder),
				webClientBuilderProvider.getIfAvailable(WebClient::builder), dynamicModelEntity);
		OpenAiChatOptions options = OpenAiChatOptions.fromOptions(defaultOptions);
		var chatModel = OpenAiChatModel.builder()
			.openAiApi(openAiApi)
			.defaultOptions(options)
			// .toolCallingManager(toolCallingManager)
			.toolExecutionEligibilityPredicate(
					openAiToolExecutionEligibilityPredicate.getIfUnique(DefaultToolExecutionEligibilityPredicate::new))
			// .retryTemplate(retryTemplate)
			.observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
			.build();

		observationConvention.ifAvailable(chatModel::setObservationConvention);

		return chatModel;
	}

	@Override
	public ChatClient getChatClientByModelId(Long modelId) {
		if (modelId == null) {
			return getDefaultChatClient();
		}

		DynamicModelEntity model = dynamicModelRepository.findById(modelId).orElse(null);
		if (model == null) {
			return getDefaultChatClient();
		}

		return getDynamicChatClient(model);
	}

	@Override
	public ChatClient getDefaultChatClient() {
		DynamicModelEntity defaultModel = dynamicModelRepository.findByIsDefaultTrue();
		if (defaultModel != null) {
			return getDynamicChatClient(defaultModel);
		}

		List<DynamicModelEntity> availableModels = dynamicModelRepository.findAll();
		if (!availableModels.isEmpty()) {
			return getDynamicChatClient(availableModels.get(0));
		}

		throw new IllegalStateException("Agent ChatClient not initialized, please specify model first");
	}

	private OpenAiApi openAiApi(RestClient.Builder restClientBuilder, WebClient.Builder webClientBuilder,
			DynamicModelEntity dynamicModelEntity) {
		Map<String, String> headers = dynamicModelEntity.getHeaders();
		MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap<>();
		if (headers != null) {
			headers.forEach((key, value) -> multiValueMap.add(key, value));
		}

		// Clone WebClient.Builder and add timeout configuration
		WebClient.Builder enhancedWebClientBuilder = webClientBuilder.clone()
			// Add 5 minutes default timeout setting
			.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
			.filter((request, next) -> next.exchange(request).timeout(Duration.ofMinutes(10)));

		String completionsPath = dynamicModelEntity.getCompletionsPath();

		return new OpenAiApi(dynamicModelEntity.getBaseUrl(), new SimpleApiKey(dynamicModelEntity.getApiKey()),
				multiValueMap, completionsPath, "/v1/embeddings", restClientBuilder, enhancedWebClientBuilder,
				RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER) {
			@Override
			public ResponseEntity<ChatCompletion> chatCompletionEntity(ChatCompletionRequest chatRequest,
					MultiValueMap<String, String> additionalHttpHeader) {
				llmTraceRecorder.recordRequest(chatRequest);
				return super.chatCompletionEntity(chatRequest, additionalHttpHeader);
			}

			@Override
			public Flux<ChatCompletionChunk> chatCompletionStream(ChatCompletionRequest chatRequest,
					MultiValueMap<String, String> additionalHttpHeader) {
				llmTraceRecorder.recordRequest(chatRequest);
				return super.chatCompletionStream(chatRequest, additionalHttpHeader);
			}
		};
	}

}

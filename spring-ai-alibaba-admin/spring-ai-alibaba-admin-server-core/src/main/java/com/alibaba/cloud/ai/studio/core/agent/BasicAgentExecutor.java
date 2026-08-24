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

package com.alibaba.cloud.ai.studio.core.agent;

import com.alibaba.cloud.ai.studio.runtime.domain.chat.ChatMessage;
import com.alibaba.cloud.ai.studio.runtime.domain.chat.ContentType;
import com.alibaba.cloud.ai.studio.runtime.domain.chat.MessageRole;
import com.alibaba.cloud.ai.studio.runtime.domain.chat.MultimodalContent;
import com.alibaba.cloud.ai.studio.runtime.domain.chat.ToolCall;
import com.alibaba.cloud.ai.studio.runtime.domain.chat.ToolCallType;
import com.alibaba.cloud.ai.studio.runtime.domain.chat.Usage;
import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.domain.agent.AgentRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.agent.AgentResponse;
import com.alibaba.cloud.ai.studio.runtime.domain.agent.AgentStatus;
import com.alibaba.cloud.ai.studio.runtime.domain.app.AgentConfig;
import com.alibaba.cloud.ai.studio.runtime.domain.app.FileSearchOptions;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.DocumentChunk;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.base.service.McpServerService;
import com.alibaba.cloud.ai.studio.core.base.service.PluginService;
import com.alibaba.cloud.ai.studio.core.base.service.ToolExecutionService;
import com.alibaba.cloud.ai.studio.core.config.CommonConfig;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.core.model.llm.ModelFactory;
import com.alibaba.cloud.ai.studio.core.base.manager.AppComponentManager;
import com.alibaba.cloud.ai.studio.core.base.manager.DocumentRetrieverManager;
import com.alibaba.cloud.ai.studio.core.base.manager.FileManager;
import com.alibaba.cloud.ai.studio.core.rag.advisor.KnowledgeBaseRetrievalAdvisor;
import com.alibaba.cloud.ai.studio.core.agent.tool.AgentToolCallback;
import com.alibaba.cloud.ai.studio.core.agent.tool.CompositeToolCallbackProvider;
import com.alibaba.cloud.ai.studio.core.agent.tool.ToolArgumentsHelper;
import com.alibaba.cloud.ai.studio.core.rag.DocumentChunkConverter;
import com.alibaba.cloud.ai.studio.core.utils.io.FileUtils;
import com.alibaba.cloud.ai.studio.core.utils.common.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.content.Media;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.studio.core.rag.RagConstants.FILE_SEARCH_CALL;
import static com.alibaba.cloud.ai.studio.core.rag.RagConstants.FILE_SEARCH_RESULT;
import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

/**
 * Basic agent executor implementation that handles agent interactions and tool
 * executions. Provides functionality for both streaming and non-streaming agent
 * responses.
 *
 * @since 1.0.0.3
 */
@Service()
@Qualifier("basicAgentExecutor")
@RequiredArgsConstructor
public class BasicAgentExecutor extends AbstractAgentExecutor {

	/** Service for executing tools */
	private final ToolExecutionService toolExecutionService;

	/** Service for managing plugins */
	private final PluginService pluginService;

	/** Service for MCP server interactions */
	private final McpServerService mcpServerService;

	/** Manager for app components */
	private final AppComponentManager appComponentManager;

	/** Manager for document retrieval */
	private final DocumentRetrieverManager documentRetrieverManager;

	/** Chat memory for conversation history */
	private final ChatMemory chatMemory;

	/** Common configuration settings */
	private final CommonConfig commonConfig;

	/** Factory for creating chat models */
	private final ModelFactory modelFactory;

	/** Manager for file operations */
	private final FileManager fileManager;

	/**
	 * Executes the agent request in streaming mode
	 * @param context The agent context
	 * @param request The agent request
	 * @return Flux of agent responses
	 */
	@Override
	public Flux<AgentResponse> streamExecute(AgentContext context, AgentRequest request) {
		// TODO: Spring AI chat client design has limited extensibility for business use,
		// consider redesigning by ourselves
		// build chat options
		RequestContext requestContext = RequestContextHolder.getRequestContext();
		AgentConfig config = context.getConfig();
		ToolCallingChatOptions chatOptions = buildChatOptions(config);

		// build tool callback provider
		ToolCallingManager toolCallingManager = ToolCallingManager.builder().build();
		CompositeToolCallbackProvider toolCallbackProvider = buildToolCallbackProvider(config, request.getExtraPrams());

		// build messages
		List<Message> messages = buildMessages(context);

		// build chat client
		ChatClient.Builder chatClientBuilder = buildChatClient(context, chatOptions, toolCallbackProvider);

		final Prompt prompt = new Prompt(messages, chatOptions);
		return chatClientBuilder.build()
			.prompt(prompt)
			.stream()
			.chatResponse()
			.concatMap(response -> processToolCallsRecursively(chatClientBuilder, response, prompt, toolCallingManager,
					toolCallbackProvider, requestContext, chatOptions));
	}

	/**
	 * Executes the agent request in non-streaming mode
	 * @param context The agent context
	 * @param request The agent request
	 * @return Agent response
	 */
	@Override
	public AgentResponse execute(AgentContext context, AgentRequest request) {
		AgentConfig config = context.getConfig();
		// build chat options
		ToolCallingChatOptions chatOptions = buildChatOptions(config);

		// build tool callback provider
		ToolCallingManager toolCallingManager = ToolCallingManager.builder().build();
		CompositeToolCallbackProvider toolCallbackProvider = buildToolCallbackProvider(config, request.getExtraPrams());

		// build messages
		List<Message> messages = buildMessages(context);

		// build chat client
		ChatClient.Builder chatClientBuilder = buildChatClient(context, chatOptions, toolCallbackProvider);

		Prompt prompt = new Prompt(messages, chatOptions);
		ChatResponse response = chatClientBuilder.build().prompt(prompt).options(chatOptions).call().chatResponse();

		Assert.notNull(response, "response can not be null");
		if (response.hasToolCalls()) {
			ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, response);

			Prompt newPrompt = new Prompt(toolExecutionResult.conversationHistory(), chatOptions);
			ChatResponse chatResponse = chatClientBuilder.build().prompt(newPrompt).call().chatResponse();

			Assert.notNull(chatResponse, "chat response can not be null");
			AgentResponse agentResponse = convertResponse(chatResponse, toolCallbackProvider).block();

			Assert.notNull(agentResponse, "agent response can not be null");

			// handle tool results
			Generation generation = response.getResults().get(0);
			List<AssistantMessage.ToolCall> assistantToolCalls = generation.getOutput().getToolCalls();
			List<ToolCall> toolCalls = new ArrayList<>(convertToolCall(assistantToolCalls, toolCallbackProvider));

			List<ToolCall> toolCallResults = convertToolResult(toolExecutionResult, toolCallbackProvider);
			toolCalls.addAll(toolCallResults);

			if (!CollectionUtils.isEmpty(toolCalls)) {
				agentResponse.getMessage().setToolCalls(toolCalls);
			}

			return agentResponse;
		}

		return convertResponse(response, toolCallbackProvider).block();
	}

	/**
	 * Builds chat options based on agent configuration
	 * @param config Agent configuration
	 * @return OpenAiChatOptions instance
	 */
	private OpenAiChatOptions buildChatOptions(AgentConfig config) {
		OpenAiChatOptions.Builder builder = OpenAiChatOptions.builder()
			.model(config.getModel())
			.streamUsage(true)
			.internalToolExecutionEnabled(false);

		if (config.getParameter() != null) {
			builder.maxTokens(config.getParameter().getMaxTokens())
				.temperature(config.getParameter().getTemperature())
				.topP(config.getParameter().getTopP())
				.presencePenalty(config.getParameter().getRepetitionPenalty());
		}

		return builder.build();
	}

	/**
	 * Creates a chat model based on configuration
	 * @param config Agent configuration
	 * @return ChatModel instance
	 */
	private ChatModel buildChatModel(AgentConfig config) {
		return modelFactory.getChatModel(config.getModelProvider());
	}

	/**
	 * Builds a chat client with necessary advisors and configurations
	 * @param context Agent context
	 * @param chatOptions Chat options
	 * @param toolCallbackProvider Tool callback provider
	 * @return ChatClient.Builder instance
	 */
	private ChatClient.Builder buildChatClient(AgentContext context, ToolCallingChatOptions chatOptions,
			ToolCallbackProvider toolCallbackProvider) {
		AgentConfig config = context.getConfig();
		AgentRequest request = context.getRequest();

		ChatModel chatModel = buildChatModel(config);
		ChatClient chatClient = ChatClient.builder(chatModel).defaultAdvisors(new SimpleLoggerAdvisor()).build();

		// Add chat memory advisor
		ChatClient.Builder chatClientBuilder = chatClient.mutate();
		if (context.isMemoryEnabled()) {
			MessageChatMemoryAdvisor advisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
			int dialogRound = config.getMemory().getDialogRound();
			chatClientBuilder.defaultAdvisors(advisor);
			String conversationId = String.format("%s_%s", context.getAppId(), request.getConversationId());
			// chatClientBuilder
			// .defaultAdvisors(memoryAdvisor -> memoryAdvisor.param(CONVERSATION_ID,
			// conversationId)
			// .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, dialogRound));
			chatClientBuilder.defaultAdvisors(memoryAdvisor -> memoryAdvisor.param(CONVERSATION_ID, conversationId));
		}

		// Add document retriever
		FileSearchOptions fileSearchOptions = config.getFileSearch();
		if (fileSearchOptions != null && fileSearchOptions.getEnableSearch()
				&& !CollectionUtils.isEmpty(fileSearchOptions.getKbIds())) {
			DocumentRetriever documentRetriever = documentRetrieverManager.getDocumentRetriever(fileSearchOptions);
			Advisor retrievalAdvisor = KnowledgeBaseRetrievalAdvisor.builder()
				.documentRetriever(documentRetriever)
				.agentContext(context)
				.commonConfig(commonConfig)
				.build();
			chatClientBuilder.defaultAdvisors(retrievalAdvisor);
		}

		// Add tool callbacks
		ToolCallback[] toolCallbacks = toolCallbackProvider.getToolCallbacks();
		if (!ArrayUtils.isEmpty(toolCallbacks)) {
			chatOptions.setToolCallbacks(Arrays.stream(toolCallbacks).toList());
		}

		return chatClientBuilder;
	}

	/**
	 * Builds message list from agent context
	 * @param context Agent context
	 * @return List of messages
	 */
	protected List<Message> buildMessages(AgentContext context) {
		AgentConfig config = context.getConfig();
		AgentRequest request = context.getRequest();

		List<Message> messages = new ArrayList<>();
		List<ChatMessage> chatMessages = request.getMessages();
		if (StringUtils.isNotBlank(config.getInstructions())
				&& !MessageRole.SYSTEM.getValue().equals(chatMessages.get(0).getRole().getValue())) {
			Message message = buildInstructions(context, config.getInstructions());
			messages.add(message);
		}

		for (ChatMessage chatMessage : chatMessages) {
			Message message = null;
			switch (chatMessage.getRole()) {
				case SYSTEM -> message = buildInstructions(context, String.valueOf(chatMessage.getContent()));
				case USER -> {
					if (chatMessage.getContentType() == ContentType.TEXT) {
						message = new UserMessage(String.valueOf(chatMessage.getContent()));
					}
					else if (chatMessage.getContentType() == ContentType.MULTIMODAL) {
						message = buildMultimodelMessage(chatMessage.getContent());
					}
					else {
						throw new BizException(ErrorCode.INVALID_PARAMS.toError("content_type",
								chatMessage.getContentType().getValue() + "not supported"));
					}
				}
				case ASSISTANT -> message = new AssistantMessage(String.valueOf(chatMessage.getContent()));
			}

			messages.add(message);
		}

		return messages;
	}

	/**
	 * Builds system instructions message
	 * @param context Agent context
	 * @param instructions System instructions
	 * @return Message instance
	 */
	private Message buildInstructions(AgentContext context, String instructions) {
		AgentConfig config = context.getConfig();
		AgentRequest request = context.getRequest();

		// Composite file search prompts
		FileSearchOptions searchOptions = config.getFileSearch();
		boolean enableFileSearch = searchOptions != null && searchOptions.getEnableSearch();
		if (enableFileSearch) {
			if (StringUtils.isBlank(instructions)) {
				instructions = commonConfig.getFileSearchPrompt();
			}

			if (searchOptions.getEnableCitation() != null && searchOptions.getEnableCitation()) {
				instructions = commonConfig.getCitationPrompt() + "\n\n" + instructions;
			}
		}

		// Handle custom prompt variables
		List<AgentConfig.PromptVariable> promptVariables = config.getPromptVariables();
		Map<String, Object> map = new HashMap<>();
		if (!CollectionUtils.isEmpty(promptVariables)) {
			for (AgentConfig.PromptVariable promptVariable : promptVariables) {
				String variableName = promptVariable.getName();
				String defaultValue = promptVariable.getDefaultValue();
				if (StringUtils.isNotBlank(variableName)) {
					map.put(variableName, defaultValue);
				}
			}

			// Override default value
			if (!CollectionUtils.isEmpty(request.getPromptVariables())) {
				for (Map.Entry<String, String> variables : request.getPromptVariables().entrySet()) {
					if (map.containsKey(variables.getKey())) {
						map.put(variables.getKey(), variables.getValue());
					}
				}
			}

			// Remove empty value
			map.keySet().removeIf(key -> {
				Object value = map.get(key);
				return value == null || StringUtils.isBlank(String.valueOf(value));
			});
		}

		if (enableFileSearch) {
			context.setPromptVariables(map);
			return new SystemMessage(instructions);
		}

		return new SystemPromptTemplate(instructions).createMessage(map);
	}

	/**
	 * Builds a multimodal message from content
	 * @param obj Content object
	 * @return Message instance
	 */
	private Message buildMultimodelMessage(Object obj) {
		if (!(obj instanceof List)) {
			throw new BizException(ErrorCode.INVALID_PARAMS.toError("content", "content must be list"));
		}

		List<MultimodalContent> content = (List<MultimodalContent>) obj;
		String text = null;
		List<Media> media = new ArrayList<>();
		try {
			for (MultimodalContent item : content) {
				switch (item.getType()) {
					case TEXT -> text = item.getText();
					case IMAGE -> {
						if (StringUtils.isNotBlank(item.getUrl())) {
							String contentType = FileUtils.getContentType(item.getUrl());
							MediaType mediaType = MediaType.parseMediaType(contentType);
							media.add(Media.builder().mimeType(mediaType).data(new URL(item.getUrl())).build());
						}
						else if (StringUtils.isNotBlank(item.getPath())) {
							String contentType = FileUtils.getContentType(item.getPath());
							MediaType mediaType = MediaType.parseMediaType(contentType);
							Resource resource = fileManager.loadFile(item.getPath());
							media.add(new Media(mediaType, resource));
						}
						else if (StringUtils.isNotBlank(item.getData())) {
							// format should be data:image/png;base64,iVBORw0KGgoSUhEUgAAA
							int semicolonIndex = item.getData().indexOf(';');
							if (semicolonIndex == -1) {
								throw new IllegalArgumentException("Invalid data format");
							}

							String mimeType = item.getData().substring(0, semicolonIndex);
							MediaType mediaType = MediaType.parseMediaType(mimeType);
							media.add(new Media(mediaType, new ByteArrayResource(item.getData().getBytes())));
						}
						else {
							throw new BizException(ErrorCode.INVALID_PARAMS.toError("content",
									"image content must be url or path or data"));
						}
					}
				}
			}
		}
		catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}

		return UserMessage.builder().text(text).media(media).build();
	}

	/**
	 * Converts chat response to agent response
	 * @param chatResponse Chat response
	 * @param toolCallbackProvider Tool callback provider
	 * @return Mono of agent response
	 */
	protected Mono<AgentResponse> convertResponse(ChatResponse chatResponse,
			CompositeToolCallbackProvider toolCallbackProvider) {
		if (chatResponse == null) {
			return Mono.empty();
		}

		org.springframework.ai.chat.metadata.Usage usage = chatResponse.getMetadata().getUsage();

		AgentResponse.AgentResponseBuilder responseBuilder = AgentResponse.builder()
			.model(chatResponse.getMetadata().getModel());

		if (!CollectionUtils.isEmpty(chatResponse.getResults())) {
			Generation generation = chatResponse.getResults().get(0);
			String finishReason = generation.getMetadata().getFinishReason();

			// Process reasoning content response
			Object reasoningContentObj = generation.getOutput().getMetadata().get("reasoningContent");
			String reasoningContent = reasoningContentObj == null ? null : String.valueOf(reasoningContentObj);
			reasoningContent = StringUtils.isBlank(reasoningContent) ? null : reasoningContent;

			ChatMessage.ChatMessageBuilder messageBuilder = ChatMessage.builder()
				.role(MessageRole.ASSISTANT)
				.content(generation.getOutput().getText())
				.reasoningContent(reasoningContent);

			List<ToolCall> toolCalls = new ArrayList<>();
			if (!CollectionUtils.isEmpty(generation.getOutput().getToolCalls())) {
				toolCalls.addAll(convertToolCall(generation.getOutput().getToolCalls(), toolCallbackProvider));
			}

			if (chatResponse.getMetadata() != null) {
				toolCalls.addAll(convertToolCall(chatResponse.getMetadata()));
			}

			if (!CollectionUtils.isEmpty(toolCalls)) {
				messageBuilder.toolCalls(toolCalls);
			}

			AgentStatus status = AgentStatus.toAgentStatus(finishReason);
			responseBuilder.status(status).message(messageBuilder.build());

			if (usage.getPromptTokens() > 0 || usage.getCompletionTokens() > 0 || usage.getTotalTokens() > 0) {
				responseBuilder.usage(Usage.builder()
					.promptTokens(usage.getPromptTokens())
					.completionTokens(usage.getCompletionTokens())
					.totalTokens(usage.getTotalTokens())
					.build());
			}
		}
		else {
			return Mono.empty();
		}

		return Mono.just(responseBuilder.build());
	}

	/**
	 * Converts tool calls to agent tool calls
	 * @param toolCalls List of assistant tool calls
	 * @param toolCallbackProvider tool callback provider
	 * @return List of tool calls
	 */
	private List<ToolCall> convertToolCall(List<AssistantMessage.ToolCall> toolCalls,
			CompositeToolCallbackProvider toolCallbackProvider) {
		if (CollectionUtils.isEmpty(toolCalls)) {
			return new ArrayList<>(0);
		}

		Map<String, AgentToolCallback> toolCallbackMap = getAgentToolCallback(toolCallbackProvider);

		return toolCalls.stream().map(toolCall -> {
			AgentToolCallback toolCallback = toolCallbackMap.get(toolCall.name());
			ToolCallType type = ToolCallType.FUNCTION;
			String arguments = toolCall.arguments();
			if (toolCallback != null) {
				type = toolCallback.getToolCallType();
				Map<String, Object> argumentsMap = ToolArgumentsHelper.mergeToolArguments(toolCall.arguments(),
						toolCallbackProvider.getExtraParams(), toolCallback.getId());
				arguments = JsonUtils.toJson(argumentsMap);
			}

			return ToolCall.builder()
				.id(toolCall.id())
				.type(type)
				.function(ToolCall.Function.builder().name(toolCall.name()).arguments(arguments).build())
				.build();
		}).collect(Collectors.toCollection(ArrayList::new));
	}

	/**
	 * Converts chat response metadata to tool calls, especially file search call
	 * @param metadata Chat response metadata
	 * @return List of tool calls
	 */
	private List<ToolCall> convertToolCall(ChatResponseMetadata metadata) {
		List<ToolCall> toolCalls = new ArrayList<>();
		if (metadata.containsKey(FILE_SEARCH_CALL)) {
			ToolCall toolCall = ToolCall.builder()
				.id(IdGenerator.uuid())
				.type(ToolCallType.FILE_SEARCH_CALL)
				.function(
						ToolCall.Function.builder().arguments(JsonUtils.toJson(metadata.get(FILE_SEARCH_CALL))).build())
				.build();
			toolCalls.add(toolCall);
		}

		if (metadata.containsKey(FILE_SEARCH_RESULT) && metadata.get(FILE_SEARCH_RESULT) != null) {
			List<Document> documents = metadata.get(FILE_SEARCH_RESULT);
			Assert.notNull(documents, "RAG_DOCUMENT_CONTEXT is not a list");

			List<DocumentChunk> chunks = new ArrayList<>();
			for (Document document : documents) {
				chunks.add(DocumentChunkConverter.toDocumentChunk(document));
			}

			ToolCall toolCall = ToolCall.builder()
				.id(IdGenerator.uuid())
				.type(ToolCallType.FILE_SEARCH_RESULT)
				.function(ToolCall.Function.builder().output(JsonUtils.toJson(chunks)).build())
				.build();
			toolCalls.add(toolCall);
		}

		return toolCalls;
	}

	/**
	 * Converts tool execution result to tool calls
	 * @param toolExecutionResult Tool execution result
	 * @param toolCallbackProvider tool callback provider
	 * @return List of tool calls
	 */
	private List<ToolCall> convertToolResult(ToolExecutionResult toolExecutionResult,
			CompositeToolCallbackProvider toolCallbackProvider) {
		List<ToolCall> toolResults = new ArrayList<>();
		List<Message> conversationHistory = toolExecutionResult.conversationHistory();

		Map<String, AgentToolCallback> toolCallbackMap = getAgentToolCallback(toolCallbackProvider);

		if (conversationHistory
			.get(conversationHistory.size() - 1) instanceof ToolResponseMessage toolResponseMessage) {
			toolResponseMessage.getResponses().forEach(response -> {
				// Map tool type to tool result type
				AgentToolCallback toolCallback = toolCallbackMap.get(response.name());
				ToolCallType type = toolCallback == null ? ToolCallType.TOOL_RESULT : toolCallback.getToolCallType();
				ToolCallType resultType;
				switch (type) {
					case MCP_TOOL_CALL -> resultType = ToolCallType.MCP_TOOL_RESULT;
					case COMPONENT_TOOL_CALL -> resultType = ToolCallType.COMPONENT_TOOL_RESULT;
					default -> resultType = ToolCallType.TOOL_RESULT;
				}

				ToolCall toolCallOutput = ToolCall.builder()
					.id(response.id())
					.type(resultType)
					.function(ToolCall.Function.builder().name(response.name()).output(response.responseData()).build())
					.build();
				toolResults.add(toolCallOutput);
			});
		}

		return toolResults;
	}

	/**
	 * Converts tool execution result to agent response
	 * @param response Chat response
	 * @param toolExecutionResult Tool execution result
	 * @param toolCallbackProvider tool callback provider
	 * @return Agent response
	 */
	protected AgentResponse convertToolResult(ChatResponse response, ToolExecutionResult toolExecutionResult,
			CompositeToolCallbackProvider toolCallbackProvider) {
		String model = response.getMetadata().getModel();
		List<ToolCall> toolResults = convertToolResult(toolExecutionResult, toolCallbackProvider);

		return AgentResponse.builder()
			.model(model)
			.message(ChatMessage.builder().role(MessageRole.ASSISTANT).content("").toolCalls(toolResults).build())
			.status(AgentStatus.IN_PROGRESS)
			.build();
	}

	/**
	 * Builds tool callback provider
	 * @param config Agent configuration
	 * @param extraParams Extra parameters
	 * @return CompositeToolCallbackProvider instance
	 */
	private CompositeToolCallbackProvider buildToolCallbackProvider(AgentConfig config,
			Map<String, Object> extraParams) {
		return new CompositeToolCallbackProvider(config, pluginService, toolExecutionService, mcpServerService,
				appComponentManager, extraParams);
	}

	private Flux<AgentResponse> processToolCallsRecursively(ChatClient.Builder chatClientBuilder, ChatResponse response,
			Prompt originalPrompt, ToolCallingManager toolCallingManager,
			CompositeToolCallbackProvider toolCallbackProvider, RequestContext requestContext,
			ToolCallingChatOptions chatOptions) {

		if (!response.hasToolCalls()) {
			return convertResponse(response, toolCallbackProvider).flux();
		}

		// Handle tool calling process
		return Flux.concat(
				// 1. Send tool call request
				convertResponse(response, toolCallbackProvider).flux(),
				// 2. Send tool execution result
				Mono.fromCallable(() -> {
					RequestContextHolder.setRequestContext(requestContext);
					try {
						ToolExecutionResult result = toolCallingManager.executeToolCalls(originalPrompt, response);
						return new Object[] { result, convertToolResult(response, result, toolCallbackProvider) };
					}
					finally {
						RequestContextHolder.clearRequestContext();
					}
				}).flux().flatMap(array -> {
					ToolExecutionResult result = (ToolExecutionResult) array[0];
					AgentResponse toolResult = (AgentResponse) array[1];

					// Create new prompt with the tool execution result
					Prompt newPrompt = new Prompt(result.conversationHistory(), chatOptions);

					return Flux.concat(
							// Send tool execution result
							Flux.just(toolResult),
							// 3. Send new prompt and recursively process any further tool
							// calls
							chatClientBuilder.build()
								.prompt(newPrompt)
								.stream()
								.chatResponse()
								// Key change: Recursively process the response
								.concatMap(newResponse -> processToolCallsRecursively(chatClientBuilder, newResponse,
										newPrompt, toolCallingManager, toolCallbackProvider, requestContext,
										chatOptions)));
				}));
	}

	/**
	 * Get agent tool callback
	 * @param toolCallbackProvider tool callback provider
	 * @return agent tool callback
	 */
	private Map<String, AgentToolCallback> getAgentToolCallback(ToolCallbackProvider toolCallbackProvider) {
		Map<String, AgentToolCallback> agentToolCallbackMap = new HashMap<>();
		ToolCallback[] toolCallbacks = toolCallbackProvider.getToolCallbacks();
		if (ArrayUtils.isEmpty(toolCallbacks)) {
			return agentToolCallbackMap;
		}

		for (ToolCallback toolCallback : toolCallbacks) {
			if (toolCallback instanceof AgentToolCallback agentToolCallback) {
				agentToolCallbackMap.put(agentToolCallback.getToolDefinition().name(), agentToolCallback);
			}
		}

		return agentToolCallbackMap;
	}

}

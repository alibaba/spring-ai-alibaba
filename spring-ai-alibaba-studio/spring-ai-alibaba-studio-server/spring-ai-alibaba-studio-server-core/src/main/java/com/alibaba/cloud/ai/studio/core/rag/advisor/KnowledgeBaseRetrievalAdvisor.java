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

package com.alibaba.cloud.ai.studio.core.rag.advisor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.alibaba.cloud.ai.studio.core.agent.AgentContext;
import com.alibaba.cloud.ai.studio.core.config.CommonConfig;
import com.alibaba.cloud.ai.studio.core.rag.RagConstants;
import com.alibaba.cloud.ai.studio.core.utils.LogUtils;
import com.alibaba.cloud.ai.studio.core.utils.common.BeanCopierUtils;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.domain.app.FileSearchOptions;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.AdvisorUtils;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.util.PromptAssert;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import static com.alibaba.cloud.ai.studio.core.rag.RagConstants.FILE_SEARCH_CALL;
import static com.alibaba.cloud.ai.studio.core.rag.RagConstants.FILE_SEARCH_RESULT;
import static com.alibaba.cloud.ai.studio.core.rag.RagConstants.REQUEST_CONTEXT;

/**
 * Knowledge base retrieval advisor that enhances chat requests with relevant document
 * context. This advisor retrieves documents based on user queries and augments the system
 * prompt with retrieved content.
 *
 * @since 1.0.0.3
 */
public class KnowledgeBaseRetrievalAdvisor implements BaseAdvisor {

	/** Document retriever for fetching relevant documents */
	private final DocumentRetriever documentRetriever;

	/** Scheduler for handling asynchronous operations */
	private final Scheduler scheduler;

	/** Order of execution in the advisor chain */
	private final int order;

	/** Context containing agent configuration and state */
	private final AgentContext agentContext;

	private KnowledgeBaseRetrievalAdvisor(DocumentRetriever documentRetriever, @Nullable Scheduler scheduler,
			Integer order, CommonConfig commonConfig, AgentContext agentContext) {
		Assert.notNull(documentRetriever, "documentRetriever cannot be null");
		Assert.notNull(documentRetriever, "documentRetriever cannot be null");
		Assert.notNull(commonConfig, "common config cannot be null");
		Assert.notNull(agentContext, "agent context cannot be null");

		this.agentContext = agentContext;
		this.documentRetriever = documentRetriever;
		this.scheduler = scheduler != null ? scheduler : BaseAdvisor.DEFAULT_SCHEDULER;
		this.order = order != null ? order : 0;
	}

	public static KnowledgeBaseRetrievalAdvisor.Builder builder() {
		return new KnowledgeBaseRetrievalAdvisor.Builder();
	}

	@Override
	public ChatClientRequest before(ChatClientRequest chatClientRequest, @Nullable AdvisorChain advisorChain) {
		// if (chatClientRequest.prompt().getUserMessage() == null) {
		// return ChatClientRequest.builder().build();
		// }

		UserMessage request = chatClientRequest.prompt().getUserMessage();

		Map<String, Object> context = new HashMap<>(chatClientRequest.context());

		// 1. build query
		FileSearchOptions searchOptions = agentContext.getConfig().getFileSearch();
		Map<String, Object> queryContext = new HashMap<>();
		queryContext.put(REQUEST_CONTEXT, BeanCopierUtils.copy(agentContext, RequestContext.class));
		queryContext.put(FILE_SEARCH_CALL, buildFileSearchRequestContext(request.getText(), searchOptions));

		Query query = Query.builder()
			.text(chatClientRequest.prompt().getUserMessage().getText())
			.history(chatClientRequest.prompt().getInstructions())
			.context(queryContext)
			.build();

		// 2. retrieve relevant documents
		List<Document> documents = this.documentRetriever.retrieve(query);

		// 3. Augment user query with the document contextual data.
		String documentContext = documents.stream()
			.map(Document::getText)
			.collect(Collectors.joining(System.lineSeparator()));

		// 3.1. Define prompt parameters.
		Map<String, Object> promptParameters = new HashMap<>();
		promptParameters.put(RagConstants.DOCUMENTS_PLACEHOLDER, documentContext);

		Map<String, Object> promptVariables = agentContext.getPromptVariables();
		LogUtils.info("query augment, prompt variables: {}", promptVariables);
		if (!CollectionUtils.isEmpty(promptVariables)) {
			promptParameters.putAll(promptVariables);
		}

		// 3.2. Augment user prompt with document context.
		SystemMessage templatedSystemMessage = chatClientRequest.prompt().getSystemMessage();

		PromptTemplate promptTemplate = new SystemPromptTemplate(templatedSystemMessage.getText());
		try {
			PromptAssert.templateHasRequiredPlaceholders(promptTemplate, RagConstants.DOCUMENTS_PLACEHOLDER);
		}
		catch (Exception e) {
			throw new BizException(
					ErrorCode.INVALID_PARAMS.toError("documents", "{documents} placeholder is missing in instructions"),
					e);
		}

		Message systemMessage = promptTemplate.createMessage(promptParameters);
		chatClientRequest.prompt()
			.getInstructions()
			.removeIf(element -> element.getMessageType() == MessageType.SYSTEM);
		chatClientRequest.prompt().getInstructions().add(0, systemMessage);

		// 4. Update advised request with augmented prompt.
		context.put(FILE_SEARCH_RESULT, documents);

		return chatClientRequest.mutate()
			.prompt(chatClientRequest.prompt().augmentUserMessage(request.getText()))
			.context(context)
			.build();
	}

	/**
	 * Processes a single query by routing it to document retrievers and collecting
	 * documents.
	 */
	private Map.Entry<Query, List<Document>> getDocumentsForQuery(Query query) {
		List<Document> documents = this.documentRetriever.retrieve(query);
		return Map.entry(query, documents);
	}

	@Override
	public ChatClientResponse after(ChatClientResponse chatClientResponse, @Nullable AdvisorChain advisorChain) {
		ChatResponse.Builder chatResponseBuilder;
		if (chatClientResponse.chatResponse() == null) {
			chatResponseBuilder = ChatResponse.builder();
		}
		else {
			chatResponseBuilder = ChatResponse.builder().from(chatClientResponse.chatResponse());
		}

		if (!agentContext.getStream()) {
			chatResponseBuilder.metadata(FILE_SEARCH_CALL, chatClientResponse.context().get(FILE_SEARCH_CALL));
			chatResponseBuilder.metadata(FILE_SEARCH_RESULT, chatClientResponse.context().get(FILE_SEARCH_RESULT));
		}
		return ChatClientResponse.builder()
			.chatResponse(chatResponseBuilder.build())
			.context(chatClientResponse.context())
			.build();
	}

	@Override
	public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain chain) {
		Assert.notNull(chatClientRequest, "advisedRequest cannot be null");
		Assert.notNull(chain, "chain cannot be null");
		Assert.notNull(getScheduler(), "scheduler cannot be null");

		Flux<ChatClientResponse> chatClientResponseFlux = Mono.just(chatClientRequest)
			.publishOn(getScheduler())
			.flatMapMany(req -> {
				// 1. emit file search option
				ChatResponse.Builder response = ChatResponse.builder()
					.generations(List.of(new Generation(new AssistantMessage(""))));
				if (chatClientRequest.prompt().getUserMessage() != null) {
					response.metadata(FILE_SEARCH_CALL,
							buildFileSearchRequestContext(chatClientRequest.prompt().getUserMessage().getText(),
									agentContext.getConfig().getFileSearch()));
				}
				ChatClientResponse preAdvice = ChatClientResponse.builder()
					.chatResponse(response.build())
					.context(chatClientRequest.context())
					.build();

				// 2. emit immediate response
				return Flux.concat(Flux.just(preAdvice),
						Mono.just(req).map(request -> this.before(request, chain)).flatMapMany(adviseReq -> {
							ChatClientResponse immediateResponse = ChatClientResponse.builder()
								.chatResponse(ChatResponse.builder()
									.generations(List.of(new Generation(new AssistantMessage(""))))
									.metadata(FILE_SEARCH_RESULT, chatClientRequest.context().get(FILE_SEARCH_RESULT))
									.build())
								.context(chatClientRequest.context())
								.build();
							return Flux.just(immediateResponse).concatWith(chain.nextStream(adviseReq));
						}));
			});

		return chatClientResponseFlux.map(ar -> {
			if (AdvisorUtils.onFinishReason().test(ar)) {
				ar = after(ar, chain);
			}
			return ar;
		}).onErrorResume(error -> Flux.error(new IllegalStateException("Stream processing failed", error)));
	}

	@Override
	public Scheduler getScheduler() {
		return this.scheduler;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	private static TaskExecutor buildDefaultTaskExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setThreadNamePrefix("ai-advisor-");
		taskExecutor.setCorePoolSize(4);
		taskExecutor.setMaxPoolSize(16);
		taskExecutor.setTaskDecorator(new ContextPropagatingTaskDecorator());
		taskExecutor.initialize();
		return taskExecutor;
	}

	public static final class Builder {

		private DocumentRetriever documentRetriever;

		private Scheduler scheduler;

		private Integer order;

		private CommonConfig commonConfig;

		private AgentContext agentContext;

		private Builder() {
		}

		public KnowledgeBaseRetrievalAdvisor.Builder documentRetriever(DocumentRetriever documentRetriever) {
			this.documentRetriever = documentRetriever;
			return this;
		}

		public KnowledgeBaseRetrievalAdvisor.Builder scheduler(Scheduler scheduler) {
			this.scheduler = scheduler;
			return this;
		}

		public KnowledgeBaseRetrievalAdvisor.Builder order(Integer order) {
			this.order = order;
			return this;
		}

		public KnowledgeBaseRetrievalAdvisor.Builder commonConfig(CommonConfig commonConfig) {
			this.commonConfig = commonConfig;
			return this;
		}

		public KnowledgeBaseRetrievalAdvisor.Builder agentContext(AgentContext agentContext) {
			this.agentContext = agentContext;
			return this;
		}

		public KnowledgeBaseRetrievalAdvisor build() {
			return new KnowledgeBaseRetrievalAdvisor(this.documentRetriever, this.scheduler, this.order,
					this.commonConfig, this.agentContext);
		}

	}

	/**
	 * Builds the file search request context with query and search options.
	 */
	private Map<String, Object> buildFileSearchRequestContext(String query, FileSearchOptions fileSearchOptions) {
		return Map.of("query", query, "search_options", fileSearchOptions);
	}

}

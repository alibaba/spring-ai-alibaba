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

import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.domain.app.FileSearchOptions;
import com.alibaba.cloud.ai.studio.core.agent.AgentContext;
import com.alibaba.cloud.ai.studio.core.config.CommonConfig;
import com.alibaba.cloud.ai.studio.core.rag.RagConstants;
import com.alibaba.cloud.ai.studio.core.utils.common.BeanCopierUtils;
import com.alibaba.cloud.ai.studio.core.utils.LogUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.util.PromptAssert;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.studio.core.rag.RagConstants.*;

/**
 * Knowledge base retrieval advisor that enhances chat requests with relevant document
 * context. This advisor retrieves documents based on user queries and augments the system
 * prompt with retrieved content.
 *
 * @since 1.0.0-M1
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

	public KnowledgeBaseRetrievalAdvisor(DocumentRetriever documentRetriever, @Nullable Scheduler scheduler,
			Integer order, CommonConfig commonConfig, AgentContext agentContext) {

		Assert.notNull(documentRetriever, "documentRetriever cannot be null");
		Assert.notNull(commonConfig, "common config cannot be null");
		Assert.notNull(agentContext, "agent context cannot be null");

		this.agentContext = agentContext;
		this.documentRetriever = documentRetriever;
		this.scheduler = scheduler != null ? scheduler : BaseAdvisor.DEFAULT_SCHEDULER;
		this.order = order != null ? order : 0;
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Processes the request before sending to the chat model. Retrieves relevant
	 * documents and augments the system prompt with document context.
	 */
	@NotNull
	@Override
	public AdvisedRequest before(AdvisedRequest request) {
		if (request.userText() == null) {
			return AdvisedRequest.from(request).build();
		}

		Map<String, Object> context = new HashMap<>(request.adviseContext());

		// 1. build query
		FileSearchOptions searchOptions = agentContext.getConfig().getFileSearch();
		Map<String, Object> queryContext = new HashMap<>();
		queryContext.put(REQUEST_CONTEXT, BeanCopierUtils.copy(agentContext, RequestContext.class));
		queryContext.put(FILE_SEARCH_CALL, buildFileSearchRequestContext(request.userText(), searchOptions));

		Query query = Query.builder()
			.text(new PromptTemplate(request.userText(), request.userParams()).render())
			.history(request.messages())
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
		String prompt = null;
		for (Message message : request.messages()) {
			if (message.getMessageType() == MessageType.SYSTEM) {
				prompt = message.getText();
			}
		}

		PromptTemplate promptTemplate = new SystemPromptTemplate(prompt);
		try {
			PromptAssert.templateHasRequiredPlaceholders(promptTemplate, RagConstants.DOCUMENTS_PLACEHOLDER);
		}
		catch (Exception e) {
			throw new BizException(
					ErrorCode.INVALID_PARAMS.toError("documents", "{documents} placeholder is missing in instructions"),
					e);
		}

		Message systemMessage = promptTemplate.createMessage(promptParameters);
		request.messages().removeIf(element -> element.getMessageType() == MessageType.SYSTEM);
		request.messages().add(0, systemMessage);

		// 4. Update advised request with augmented prompt.
		context.put(FILE_SEARCH_RESULT, documents);
		return AdvisedRequest.from(request).adviseContext(context).build();
	}

	/**
	 * Processes the response after receiving from the chat model. Adds metadata about
	 * file search results to the response.
	 */
	@NotNull
	@Override
	public AdvisedResponse after(AdvisedResponse advisedResponse) {
		ChatResponse.Builder chatResponseBuilder;
		if (advisedResponse.response() == null) {
			chatResponseBuilder = ChatResponse.builder();
		}
		else {
			chatResponseBuilder = ChatResponse.builder().from(advisedResponse.response());
		}

		if (!agentContext.getStream()) {
			chatResponseBuilder.metadata(FILE_SEARCH_CALL, advisedResponse.adviseContext().get(FILE_SEARCH_CALL));
			chatResponseBuilder.metadata(FILE_SEARCH_RESULT, advisedResponse.adviseContext().get(FILE_SEARCH_RESULT));
		}
		return new AdvisedResponse(chatResponseBuilder.build(), advisedResponse.adviseContext());
	}

	/**
	 * Handles streaming responses by emitting file search options and results before the
	 * actual response.
	 */
	@NotNull
	@Override
	public Flux<AdvisedResponse> aroundStream(@NotNull AdvisedRequest advisedRequest,
			@NotNull StreamAroundAdvisorChain chain) {
		Assert.notNull(advisedRequest, "advisedRequest cannot be null");
		Assert.notNull(chain, "chain cannot be null");
		Assert.notNull(getScheduler(), "scheduler cannot be null");

		Flux<AdvisedResponse> advisedResponses = Mono.just(advisedRequest)
			.publishOn(getScheduler())
			.flatMapMany(req -> {
				// 1. emit file search option
				ChatResponse.Builder response = ChatResponse.builder()
					.generations(List.of(new Generation(new AssistantMessage(""))));
				if (advisedRequest.userText() != null) {
					response.metadata(FILE_SEARCH_CALL, buildFileSearchRequestContext(advisedRequest.userText(),
							agentContext.getConfig().getFileSearch()));
				}
				AdvisedResponse preAdvice = new AdvisedResponse(response.build(), req.adviseContext());

				// 2. emit immediate response
				return Flux.concat(Flux.just(preAdvice), Mono.just(req).map(this::before).flatMapMany(adviseReq -> {
					AdvisedResponse immediateResponse = new AdvisedResponse(ChatResponse.builder()
						.generations(List.of(new Generation(new AssistantMessage(""))))
						.metadata(FILE_SEARCH_RESULT, adviseReq.adviseContext().get(FILE_SEARCH_RESULT))
						.build(), adviseReq.adviseContext());
					return Flux.just(immediateResponse).concatWith(chain.nextAroundStream(adviseReq));
				}));
			});

		return advisedResponses.map(ar -> {
			if (onFinishReason().test(ar)) {
				ar = after(ar);
			}
			return ar;
		});
	}

	private Predicate<AdvisedResponse> onFinishReason() {
		return advisedResponse -> {
			ChatResponse chatResponse = advisedResponse.response();
			return chatResponse != null && chatResponse.getResults() != null && chatResponse.getResults()
				.stream()
				.anyMatch(result -> result != null && result.getMetadata() != null
						&& org.springframework.util.StringUtils.hasText(result.getMetadata().getFinishReason()));
		};
	}

	@NotNull
	@Override
	public Scheduler getScheduler() {
		return this.scheduler;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * Builder class for constructing KnowledgeBaseRetrievalAdvisor instances.
	 */
	public static final class Builder {

		private DocumentRetriever documentRetriever;

		private Scheduler scheduler;

		private Integer order;

		private CommonConfig commonConfig;

		private AgentContext agentContext;

		private Builder() {
		}

		public Builder documentRetriever(DocumentRetriever documentRetriever) {
			this.documentRetriever = documentRetriever;
			return this;
		}

		public Builder scheduler(Scheduler scheduler) {
			this.scheduler = scheduler;
			return this;
		}

		public Builder order(Integer order) {
			this.order = order;
			return this;
		}

		public Builder commonConfig(CommonConfig commonConfig) {
			this.commonConfig = commonConfig;
			return this;
		}

		public Builder agentContext(AgentContext agentContext) {
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

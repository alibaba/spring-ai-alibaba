/*
 * Copyright 2024-2025 the original author or authors.
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

package com.alibaba.cloud.ai.advisor;

import com.alibaba.cloud.ai.document.DocumentWithScore;
import com.alibaba.cloud.ai.model.RerankModel;
import com.alibaba.cloud.ai.model.RerankRequest;
import com.alibaba.cloud.ai.model.RerankResponse;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionTextParser;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * Title Content rerank advisor.<br>
 * Description Content rerank advisor.<br>
 *
 * @author yuanci.ytb
 * @since 1.0.0-M2
 */

public class RetrievalRerankAdvisor implements CallAdvisor, StreamAdvisor {

	private static final Logger logger = LoggerFactory.getLogger(RetrievalRerankAdvisor.class);

	private static final String DEFAULT_USER_TEXT_ADVISE = """
			Context information is below.
			---------------------
			{question_answer_context}
			---------------------
			Given the context and provided history information and not prior knowledge,
			reply to the user comment. If the answer is not in the context, inform
			the user that you can't answer the question.
			""";

	private static final Double DEFAULT_MIN_SCORE = 0.1;

	private static final int DEFAULT_ORDER = 0;

	private final VectorStore vectorStore;

	private final RerankModel rerankModel;

	private final String userTextAdvise;

	private final SearchRequest searchRequest;

	private final Double minScore;

	private final boolean protectFromBlocking;

	private final int order;

	public static final String RETRIEVED_DOCUMENTS = "qa_retrieved_documents";

	public static final String FILTER_EXPRESSION = "qa_filter_expression";

	public static final String RERANK_SCORE = "rerank_score";

	public RetrievalRerankAdvisor(VectorStore vectorStore, RerankModel rerankModel) {
		this(vectorStore, rerankModel, SearchRequest.builder().build(), DEFAULT_USER_TEXT_ADVISE, DEFAULT_MIN_SCORE);
	}

	public RetrievalRerankAdvisor(VectorStore vectorStore, RerankModel rerankModel, Double score) {
		this(vectorStore, rerankModel, SearchRequest.builder().build(), DEFAULT_USER_TEXT_ADVISE, score);
	}

	public RetrievalRerankAdvisor(VectorStore vectorStore, RerankModel rerankModel, SearchRequest searchRequest) {
		this(vectorStore, rerankModel, searchRequest, DEFAULT_USER_TEXT_ADVISE, DEFAULT_MIN_SCORE);
	}

	public RetrievalRerankAdvisor(VectorStore vectorStore, RerankModel rerankModel, SearchRequest searchRequest,
			String userTextAdvise, Double minScore) {
		this(vectorStore, rerankModel, searchRequest, userTextAdvise, minScore, true);
	}

	public RetrievalRerankAdvisor(VectorStore vectorStore, RerankModel rerankModel, SearchRequest searchRequest,
			String userTextAdvise, Double minScore, boolean protectFromBlocking) {
		this(vectorStore, rerankModel, searchRequest, userTextAdvise, minScore, protectFromBlocking, DEFAULT_ORDER);
	}

	public RetrievalRerankAdvisor(VectorStore vectorStore, RerankModel rerankModel, SearchRequest searchRequest,
			String userTextAdvise, Double minScore, boolean protectFromBlocking, int order) {
		Assert.notNull(vectorStore, "The vectorStore must not be null!");
		Assert.notNull(rerankModel, "The rerankModel must not be null!");
		Assert.notNull(searchRequest, "The searchRequest must not be null!");
		Assert.hasText(userTextAdvise, "The userTextAdvise must not be empty!");

		this.vectorStore = vectorStore;
		this.rerankModel = rerankModel;
		this.userTextAdvise = userTextAdvise;
		this.searchRequest = searchRequest;
		this.minScore = minScore;
		this.protectFromBlocking = protectFromBlocking;
		this.order = order;
	}

	@Override
	public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
		chatClientRequest = this.before(chatClientRequest);

		ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);

		return this.after(chatClientResponse);
	}

	@Override
	public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest,
			StreamAdvisorChain streamAdvisorChain) {
		// This can be executed by both blocking and non-blocking Threads
		// E.g. a command line or Tomcat blocking Thread implementation
		// or by a WebFlux dispatch in a non-blocking manner.
		Flux<ChatClientResponse> advisedResponses = (this.protectFromBlocking) ?
		// @formatter:off
				Mono.just(chatClientRequest)
						.publishOn(Schedulers.boundedElastic())
						.map(this::before)
						.flatMapMany(request -> streamAdvisorChain.nextStream(request))
				: streamAdvisorChain.nextStream(this.before(chatClientRequest));
		// @formatter:on

		return advisedResponses.map(ar -> {
			if (onFinishReason().test(ar)) {
				ar = after(ar);
			}
			return ar;
		});
	}

	@NotNull
	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	protected Filter.Expression doGetFilterExpression(Map<String, Object> context) {

		if (!context.containsKey(FILTER_EXPRESSION)
				|| !StringUtils.hasText(context.get(FILTER_EXPRESSION).toString())) {
			return this.searchRequest.getFilterExpression();
		}
		return new FilterExpressionTextParser().parse(context.get(FILTER_EXPRESSION).toString());

	}

	protected List<Document> doRerank(ChatClientRequest request, List<Document> documents) {
		if (CollectionUtils.isEmpty(documents)) {
			return documents;
		}
		String userText = "";
		List<Message> messages = new LinkedList<>(request.prompt().getInstructions());
		if (!messages.isEmpty() && messages.get(messages.size() - 1) instanceof UserMessage userMessage) {
			userText = userMessage.getText();
		}
		var rerankRequest = new RerankRequest(userText, documents);
		RerankResponse response = rerankModel.call(rerankRequest);
		logger.debug("reranked documents: {}", response);
		if (response == null || response.getResults() == null) {
			return documents;
		}

		return response.getResults()
			.stream()
			.filter(doc -> doc != null && doc.getScore() >= minScore)
			.sorted(Comparator.comparingDouble(DocumentWithScore::getScore).reversed())
			.map(DocumentWithScore::getOutput)
			.collect(toList());
	}

	private ChatClientRequest before(ChatClientRequest request) {
		// TODO 需要补充新版本实现
		return request;
	}

	private ChatClientResponse after(ChatClientResponse advisedResponse) {
		// fix meta data loss issue since ChatResponse.from won't copy meta info like id,
		// model, usage, etc. This will
		// be changed once new version of spring ai core is updated.
		ChatResponseMetadata.Builder metadataBuilder = ChatResponseMetadata.builder();
		metadataBuilder.keyValue(RETRIEVED_DOCUMENTS, advisedResponse.context().get(RETRIEVED_DOCUMENTS));

		ChatResponseMetadata metadata = advisedResponse.chatResponse().getMetadata();
		if (metadata != null) {
			metadataBuilder.id(metadata.getId());
			metadataBuilder.model(metadata.getModel());
			metadataBuilder.usage(metadata.getUsage());
			metadataBuilder.promptMetadata(metadata.getPromptMetadata());
			metadataBuilder.rateLimit(metadata.getRateLimit());

			Set<Map.Entry<String, Object>> entries = metadata.entrySet();
			for (Map.Entry<String, Object> entry : entries) {
				metadataBuilder.keyValue(entry.getKey(), entry.getValue());
			}
		}

		ChatResponse chatResponse = new ChatResponse(advisedResponse.chatResponse().getResults(),
				metadataBuilder.build());
		return new ChatClientResponse(chatResponse, advisedResponse.context());
	}

	/**
	 * Controls whether {@link RetrievalRerankAdvisor#after)} should be executed.<br />
	 * Called only on Flux elements that contain a finish reason. Usually the last element
	 * in the Flux. The response advisor can modify the elements before they are returned
	 * to the client.<br />
	 * Inspired by {@link RetrievalRerankAdvisor}.
	 */
	private Predicate<ChatClientResponse> onFinishReason() {

		return (advisedResponse) -> advisedResponse.chatResponse()
			.getResults()
			.stream()
			.anyMatch(result -> result != null && result.getMetadata() != null
					&& StringUtils.hasText(result.getMetadata().getFinishReason()));
	}

}

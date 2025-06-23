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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.alibaba.cloud.ai.document.DocumentWithScore;
import com.alibaba.cloud.ai.model.RerankModel;
import com.alibaba.cloud.ai.model.RerankRequest;
import com.alibaba.cloud.ai.model.RerankResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionTextParser;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Title Content rerank advisor.<br>
 * Description Content rerank advisor.<br>
 *
 * @author yuanci.ytb
 * @since 1.0.0-M2
 */

public class RetrievalRerankAdvisor implements BaseAdvisor {

	private static final Logger logger = LoggerFactory.getLogger(RetrievalRerankAdvisor.class);

	private static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = new PromptTemplate("""
			{query}

			Context information is below, surrounded by ---------------------
			---------------------
			{question_answer_context}
			---------------------
			Given the context and provided history information and not prior knowledge,
			reply to the user comment. If the answer is not in the context, inform
			the user that you can't answer the question.
			""");

	private static final Double DEFAULT_MIN_SCORE = 0.1;

	private static final int DEFAULT_ORDER = 0;

	private final VectorStore vectorStore;

	private final RerankModel rerankModel;

	private final PromptTemplate promptTemplate;

	private final SearchRequest searchRequest;

	private final Double minScore;

	private final int order;

	public static final String RETRIEVED_DOCUMENTS = "qa_retrieved_documents";

	public static final String FILTER_EXPRESSION = "qa_filter_expression";

	public RetrievalRerankAdvisor(VectorStore vectorStore, RerankModel rerankModel) {
		this(vectorStore, rerankModel, SearchRequest.builder().build(), DEFAULT_PROMPT_TEMPLATE, DEFAULT_MIN_SCORE);
	}

	public RetrievalRerankAdvisor(VectorStore vectorStore, RerankModel rerankModel, Double score) {
		this(vectorStore, rerankModel, SearchRequest.builder().build(), DEFAULT_PROMPT_TEMPLATE, score);
	}

	public RetrievalRerankAdvisor(VectorStore vectorStore, RerankModel rerankModel, SearchRequest searchRequest) {
		this(vectorStore, rerankModel, searchRequest, DEFAULT_PROMPT_TEMPLATE, DEFAULT_MIN_SCORE);
	}

	public RetrievalRerankAdvisor(VectorStore vectorStore, RerankModel rerankModel, SearchRequest searchRequest,
			PromptTemplate promptTemplate, Double minScore) {
		this(vectorStore, rerankModel, searchRequest, promptTemplate, minScore, DEFAULT_ORDER);
	}

	public RetrievalRerankAdvisor(VectorStore vectorStore, RerankModel rerankModel, SearchRequest searchRequest,
			PromptTemplate promptTemplate, Double minScore, int order) {
		Assert.notNull(vectorStore, "The vectorStore must not be null!");
		Assert.notNull(rerankModel, "The rerankModel must not be null!");
		Assert.notNull(searchRequest, "The searchRequest must not be null!");
		Assert.notNull(promptTemplate, "The userTextAdvise must not be null!");

		this.vectorStore = vectorStore;
		this.rerankModel = rerankModel;
		this.promptTemplate = promptTemplate;
		this.searchRequest = searchRequest;
		this.minScore = minScore;
		this.order = order;
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

		var rerankRequest = new RerankRequest(request.prompt().getUserMessage().getText(), documents);

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
			.collect(Collectors.toList());
	}

	@Override
	public ChatClientRequest before(ChatClientRequest request, AdvisorChain advisorChain) {

		var context = request.context();
		var userMessage = request.prompt().getUserMessage();

		var searchRequestToUse = SearchRequest.from(this.searchRequest)
			.query(userMessage.getText())
			.filterExpression(doGetFilterExpression(context))
			.build();

		List<Document> documents = this.vectorStore.similaritySearch(searchRequestToUse);
		context.put(RETRIEVED_DOCUMENTS, documents);

		documents = doRerank(request, documents);

		String documentContext = documents.stream()
			.map(Document::getText)
			.collect(Collectors.joining(System.lineSeparator()));

		String augmentedUserText = this.promptTemplate
			.render(Map.of("query", userMessage.getText(), "question_answer_context", documentContext));

		// Update ChatClientRequest with augmented prompt.
		return request.mutate().prompt(request.prompt().augmentUserMessage(augmentedUserText)).context(context).build();
	}

	@Override
	public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
		ChatResponse.Builder chatResponseBuilder;
		if (chatClientResponse.chatResponse() == null) {
			chatResponseBuilder = ChatResponse.builder();
		}
		else {
			chatResponseBuilder = ChatResponse.builder().from(chatClientResponse.chatResponse());
		}
		chatResponseBuilder.metadata(RETRIEVED_DOCUMENTS, chatClientResponse.context().get(RETRIEVED_DOCUMENTS));
		return ChatClientResponse.builder()
			.chatResponse(chatResponseBuilder.build())
			.context(chatClientResponse.context())
			.build();
	}

}

/*
* Copyright 2024 the original author or authors.
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
import com.alibaba.cloud.ai.model.RerankResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.AdvisedRequest;
import org.springframework.ai.chat.client.RequestResponseAdvisor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.Content;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionTextParser;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * Title Content rerank advisor.<br>
 * Description Content rerank advisor.<br>
 *
 * @author yuanci.ytb
 * @since 1.0.0-M2
 */

public class RetrievalRerankAdvisor implements RequestResponseAdvisor {

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

	private final VectorStore vectorStore;

	private final RerankModel rerankModel;

	private final String userTextAdvise;

	private final SearchRequest searchRequest;

	private final Double minScore;

	public static final String RETRIEVED_DOCUMENTS = "qa_retrieved_documents";

	public static final String FILTER_EXPRESSION = "qa_filter_expression";

	public static final String RERANK_SCORE = "rerank_score";

	public RetrievalRerankAdvisor(VectorStore vectorStore, RerankModel rerankModel) {
		this(vectorStore, rerankModel, SearchRequest.defaults(), DEFAULT_USER_TEXT_ADVISE, DEFAULT_MIN_SCORE);
	}

	public RetrievalRerankAdvisor(VectorStore vectorStore, RerankModel rerankModel, Double score) {
		this(vectorStore, rerankModel, SearchRequest.defaults(), DEFAULT_USER_TEXT_ADVISE, score);
	}

	public RetrievalRerankAdvisor(VectorStore vectorStore, RerankModel rerankModel, SearchRequest searchRequest) {
		this(vectorStore, rerankModel, searchRequest, DEFAULT_USER_TEXT_ADVISE, DEFAULT_MIN_SCORE);
	}

	public RetrievalRerankAdvisor(VectorStore vectorStore, RerankModel rerankModel, SearchRequest searchRequest,
			String userTextAdvise, Double minScore) {
		Assert.notNull(vectorStore, "The vectorStore must not be null!");
		Assert.notNull(rerankModel, "The rerankModel must not be null!");
		Assert.notNull(searchRequest, "The searchRequest must not be null!");
		Assert.hasText(userTextAdvise, "The userTextAdvise must not be empty!");

		this.vectorStore = vectorStore;
		this.rerankModel = rerankModel;
		this.userTextAdvise = userTextAdvise;
		this.searchRequest = searchRequest;
		this.minScore = minScore;
	}

	@Override
	public AdvisedRequest adviseRequest(AdvisedRequest request, Map<String, Object> context) {
		// 1. Advise the system text.
		String advisedUserText = request.userText() + System.lineSeparator() + this.userTextAdvise;

		var searchRequestToUse = SearchRequest.from(this.searchRequest)
			.withQuery(request.userText())
			.withFilterExpression(doGetFilterExpression(context));

		// 2. Search for similar documents in the vector store.
		logger.debug("searchRequestToUse: {}", searchRequestToUse);
		List<Document> documents = this.vectorStore.similaritySearch(searchRequestToUse);
		logger.debug("retrieved documents: {}", documents);

		// 3. Rerank documents for query
		documents = doRerank(request, documents);

		context.put(RETRIEVED_DOCUMENTS, documents);

		// 4. Create the context from the documents.
		String documentContext = documents.stream()
			.map(Content::getContent)
			.collect(Collectors.joining(System.lineSeparator()));

		// 5. Advise the user parameters.
		Map<String, Object> advisedUserParams = new HashMap<>(request.userParams());
		advisedUserParams.put("question_answer_context", documentContext);

		return AdvisedRequest.from(request).withUserText(advisedUserText).withUserParams(advisedUserParams).build();
	}

	@Override
	public ChatResponse adviseResponse(ChatResponse response, Map<String, Object> context) {
		ChatResponse.Builder chatResponseBuilder = ChatResponse.builder().from(response);
		chatResponseBuilder.withMetadata(RETRIEVED_DOCUMENTS, context.get(RETRIEVED_DOCUMENTS));
		return chatResponseBuilder.build();
	}

	@Override
	public Flux<ChatResponse> adviseResponse(Flux<ChatResponse> fluxResponse, Map<String, Object> context) {
		return fluxResponse.map(cr -> {
			ChatResponse.Builder chatResponseBuilder = ChatResponse.builder().from(cr);
			chatResponseBuilder.withMetadata(RETRIEVED_DOCUMENTS, context.get(RETRIEVED_DOCUMENTS));
			return chatResponseBuilder.build();
		});
	}

	protected Filter.Expression doGetFilterExpression(Map<String, Object> context) {

		if (!context.containsKey(FILTER_EXPRESSION)
				|| !StringUtils.hasText(context.get(FILTER_EXPRESSION).toString())) {
			return this.searchRequest.getFilterExpression();
		}
		return new FilterExpressionTextParser().parse(context.get(FILTER_EXPRESSION).toString());

	}

	protected List<Document> doRerank(AdvisedRequest request, List<Document> documents) {
		if (CollectionUtils.isEmpty(documents)) {
			return documents;
		}

		RerankResponse response = rerankModel.rerank(request.userText(), documents);
		logger.debug("reranked documents: {}", response);
		if (response == null || response.getDocuments() == null) {
			return documents;
		}

		return response.getDocuments()
			.stream()
			.filter(doc -> doc != null && doc.getScore() >= minScore)
			.sorted(Comparator.comparingDouble(DocumentWithScore::getScore).reversed())
			.map(DocumentWithScore::getDocument)
			.collect(toList());
	}

}

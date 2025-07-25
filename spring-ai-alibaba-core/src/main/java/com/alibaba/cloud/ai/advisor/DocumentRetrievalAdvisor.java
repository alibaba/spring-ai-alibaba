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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.util.Assert;

public class DocumentRetrievalAdvisor implements BaseAdvisor {

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

	private static final int DEFAULT_ORDER = 0;

	public static String RETRIEVED_DOCUMENTS = "question_answer_context";

	private final DocumentRetriever retriever;

	private final PromptTemplate promptTemplate;

	private final int order;

	public DocumentRetrievalAdvisor(DocumentRetriever retriever) {
		this(retriever, DEFAULT_PROMPT_TEMPLATE);
	}

	public DocumentRetrievalAdvisor(DocumentRetriever retriever, PromptTemplate promptTemplate) {
		this(retriever, promptTemplate, DEFAULT_ORDER);
	}

	public DocumentRetrievalAdvisor(DocumentRetriever retriever, PromptTemplate promptTemplate, int order) {
		Assert.notNull(retriever, "The retriever must not be null!");
		Assert.notNull(promptTemplate, "The promptTemplate must not be null!");

		this.retriever = retriever;
		this.promptTemplate = promptTemplate;
		this.order = order;
	}

	public DocumentRetrievalAdvisor(List<DocumentRetriever> retrievers) {
		this(retrievers, DEFAULT_PROMPT_TEMPLATE, DEFAULT_ORDER);
	}

	public DocumentRetrievalAdvisor(List<DocumentRetriever> retrievers, PromptTemplate promptTemplate) {
		this(retrievers, promptTemplate, DEFAULT_ORDER);
	}

	public DocumentRetrievalAdvisor(List<DocumentRetriever> retrievers, PromptTemplate promptTemplate, int order) {
		Assert.notEmpty(retrievers, "The retrievers list must not be null or empty!");
		Assert.notNull(promptTemplate, "The promptTemplate must not be null!");

		// Create a composite retriever for multiple vector stores
		this.retriever = new CompositeDocumentRetriever(retrievers);
		this.promptTemplate = promptTemplate;
		this.order = order;
	}

	/**
	 * Constructor for multiple vector stores with custom merge strategy
	 * @param retrievers List of document retrievers for multi-vector store support
	 * @param mergeStrategy Strategy for merging results from multiple retrievers
	 * @param maxResultsPerRetriever Maximum results per retriever
	 */
	public DocumentRetrievalAdvisor(List<DocumentRetriever> retrievers,
			CompositeDocumentRetriever.ResultMergeStrategy mergeStrategy, int maxResultsPerRetriever) {
		this(retrievers, mergeStrategy, maxResultsPerRetriever, DEFAULT_PROMPT_TEMPLATE, DEFAULT_ORDER);
	}

	/**
	 * Constructor for multiple vector stores with full customization
	 * @param retrievers List of document retrievers for multi-vector store support
	 * @param mergeStrategy Strategy for merging results from multiple retrievers
	 * @param maxResultsPerRetriever Maximum results per retriever
	 * @param promptTemplate Custom prompt template
	 * @param order Advisor execution order
	 */
	public DocumentRetrievalAdvisor(List<DocumentRetriever> retrievers,
			CompositeDocumentRetriever.ResultMergeStrategy mergeStrategy, int maxResultsPerRetriever,
			PromptTemplate promptTemplate, int order) {
		Assert.notEmpty(retrievers, "The retrievers list must not be null or empty!");
		Assert.notNull(promptTemplate, "The promptTemplate must not be null!");

		// Create a composite retriever for multiple vector stores with custom settings
		this.retriever = new CompositeDocumentRetriever(retrievers, maxResultsPerRetriever, mergeStrategy);
		this.promptTemplate = promptTemplate;
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public ChatClientRequest before(ChatClientRequest request, AdvisorChain advisorChain) {

		var context = request.context();
		var userMessage = request.prompt().getUserMessage();
		Query query = new Query(userMessage.getText(), request.prompt().getInstructions(), context);
		List<Document> documents = retriever.retrieve(query);
		context.put(RETRIEVED_DOCUMENTS, documents);

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

	public static class CompositeDocumentRetriever implements DocumentRetriever {

		private final List<DocumentRetriever> retrievers;

		private final int maxResultsPerRetriever;

		private final ResultMergeStrategy mergeStrategy;

		public enum ResultMergeStrategy {

			SIMPLE_MERGE,

			SCORE_BASED,

			ROUND_ROBIN

		}

		public CompositeDocumentRetriever(List<DocumentRetriever> retrievers) {
			this(retrievers, 10, ResultMergeStrategy.SCORE_BASED);
		}

		public CompositeDocumentRetriever(List<DocumentRetriever> retrievers, int maxResultsPerRetriever) {
			this(retrievers, maxResultsPerRetriever, ResultMergeStrategy.SCORE_BASED);
		}

		public CompositeDocumentRetriever(List<DocumentRetriever> retrievers, int maxResultsPerRetriever,
				ResultMergeStrategy mergeStrategy) {
			Assert.notNull(retrievers, "Retrievers list must not be null!");
			Assert.isTrue(!retrievers.isEmpty(), "Retrievers list must not be empty!");
			Assert.isTrue(maxResultsPerRetriever > 0, "MaxResultsPerRetriever must be positive!");
			Assert.notNull(mergeStrategy, "MergeStrategy must not be null!");

			this.retrievers = new ArrayList<>(retrievers);
			this.maxResultsPerRetriever = maxResultsPerRetriever;
			this.mergeStrategy = mergeStrategy;
		}

		@Override
		public List<Document> retrieve(Query query) {
			if (mergeStrategy == ResultMergeStrategy.ROUND_ROBIN) {
				return roundRobinRetrieve(query);
			}

			List<Document> allDocuments = new ArrayList<>();

			for (DocumentRetriever retriever : retrievers) {
				try {
					List<Document> documents = retriever.retrieve(query);
					if (documents != null && !documents.isEmpty()) {
						List<Document> limitedDocuments = documents.stream()
							.limit(maxResultsPerRetriever)
							.collect(Collectors.toList());
						allDocuments.addAll(limitedDocuments);
					}
				}
				catch (Exception e) {
					System.err.println("Error retrieving from one of the retrievers: " + e.getMessage());
				}
			}

			return mergeResults(allDocuments);
		}

		private List<Document> roundRobinRetrieve(Query query) {
			List<List<Document>> allResults = new ArrayList<>();

			for (DocumentRetriever retriever : retrievers) {
				try {
					List<Document> documents = retriever.retrieve(query);
					if (documents != null && !documents.isEmpty()) {

						List<Document> limitedDocuments = documents.stream()
							.limit(maxResultsPerRetriever)
							.collect(Collectors.toList());
						allResults.add(limitedDocuments);
					}
					else {
						allResults.add(new ArrayList<>());
					}
				}
				catch (Exception e) {
					System.err.println("Error retrieving from one of the retrievers: " + e.getMessage());
					allResults.add(new ArrayList<>());
				}
			}

			List<Document> result = new ArrayList<>();
			int maxSize = allResults.stream().mapToInt(List::size).max().orElse(0);

			for (int i = 0; i < maxSize; i++) {
				for (List<Document> documents : allResults) {
					if (i < documents.size()) {
						result.add(documents.get(i));
					}
				}
			}

			return result;
		}

		private List<Document> mergeResults(List<Document> documents) {
			if (documents.isEmpty()) {
				return documents;
			}

			switch (mergeStrategy) {
				case SIMPLE_MERGE:
					return documents;
				case SCORE_BASED:
					return documents.stream().sorted((d1, d2) -> {
						Double score1 = d1.getScore();
						Double score2 = d2.getScore();

						if (score1 == null)
							score1 = 0.0;
						if (score2 == null)
							score2 = 0.0;
						return Double.compare(score2, score1);
					}).collect(Collectors.toList());
				case ROUND_ROBIN:
					return documents;
				default:
					return documents;
			}
		}

		public static class Builder {

			private List<DocumentRetriever> retrievers = new ArrayList<>();

			private int maxResultsPerRetriever = 10;

			private ResultMergeStrategy mergeStrategy = ResultMergeStrategy.SCORE_BASED;

			public Builder addRetriever(DocumentRetriever retriever) {
				if (retriever != null) {
					this.retrievers.add(retriever);
				}
				return this;
			}

			public Builder retrievers(List<DocumentRetriever> retrievers) {
				if (retrievers != null) {
					this.retrievers.addAll(retrievers);
				}
				return this;
			}

			public Builder maxResultsPerRetriever(int maxResultsPerRetriever) {
				this.maxResultsPerRetriever = maxResultsPerRetriever;
				return this;
			}

			public Builder mergeStrategy(ResultMergeStrategy mergeStrategy) {
				this.mergeStrategy = mergeStrategy;
				return this;
			}

			public CompositeDocumentRetriever build() {
				return new CompositeDocumentRetriever(retrievers, maxResultsPerRetriever, mergeStrategy);
			}

		}

		public static Builder builder() {
			return new Builder();
		}

	}

}

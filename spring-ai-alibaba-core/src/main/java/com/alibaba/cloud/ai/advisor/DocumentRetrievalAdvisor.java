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

/**
 * Title Document retrieval advisor.<br>
 * Description Document retrieval advisor.<br>
 *
 * @author yuanci.ytb
 * @since 1.0.0-M2
 */
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

}
